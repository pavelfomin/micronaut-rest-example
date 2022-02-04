package com.droidablebee.micronaut.rest.endpoint;

import com.droidablebee.micronaut.rest.domain.Person;
import com.droidablebee.micronaut.rest.repository.PersonRepository;
import com.droidablebee.micronaut.rest.service.PersonService;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.core.type.Argument;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.runtime.config.DataConfiguration.PageableConfiguration;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.security.token.jwt.render.BearerAccessRefreshToken;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Date;
import java.util.UUID;

import static com.droidablebee.micronaut.rest.endpoint.PersonEndpoint.PERSON;
import static com.droidablebee.micronaut.rest.security.AuthenticationProviderUserPassword.USER_WITHOUT_ROLES;
import static com.droidablebee.micronaut.rest.security.AuthenticationProviderUserPassword.USER_WITH_READ_ROLE;
import static com.droidablebee.micronaut.rest.security.AuthenticationProviderUserPassword.USER_WITH_WRITE_ROLE;
import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@MicronautTest
class PersonEndpointTest extends BaseEndpointTest {

    @Inject
    EntityManager entityManager;

    @Inject
    PersonService personService;

    @Inject
    PersonRepository personRepository;

    private Person testPerson;
    private long timestamp;

    @BeforeEach
    void beforeEach() {

        timestamp = new Date().getTime();

        entityManager.clear();

        // create test persons
        personService.save(createPerson("Jack", "Bauer"));
        personService.save(createPerson("Chloe", "O'Brian"));
        personService.save(createPerson("Kim", "Bauer"));
        personService.save(createPerson("David", "Palmer"));
        personService.save(createPerson("Michelle", "Dessler"));

        Page<Person> persons = personService.findAll(Pageable.from(0));
        assertNotNull(persons);
        assertEquals(5L, persons.getTotalSize());

        testPerson = persons.getContent().get(0);

        //refresh entity with any changes that have been done during persistence including Hibernate conversion
        //example: java.util.Date field is injected with either with java.sql.Date (if @Temporal(TemporalType.DATE) is used)
        //or java.sql.Timestamp
        entityManager.refresh(testPerson);
    }

    @AfterEach
    void afterEach() {

        personRepository.deleteAll();
    }

    /**
     * See https://micronaut-projects.github.io/micronaut-security/latest/guide/#rejectNotFound
     */
    @Test
    void getPersonInvalidRoute() {

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(PERSON + "invalid"),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    void getPersonByIdUnauthorizedNoToken() {

        Long id = testPerson.getId();

        URI uri = UriBuilder.of(PersonEndpoint.PERSON_BY_ID).expand(singletonMap(PersonEndpoint.ID, id));
        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(uri),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    void getPersonByIdForbiddenInvalidScope() {

        Long id = testPerson.getId();

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITHOUT_ROLES));

        URI uri = UriBuilder.of(PersonEndpoint.PERSON_BY_ID).expand(singletonMap(PersonEndpoint.ID, id));
        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(uri).bearerAuth(refreshToken.getAccessToken()),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
    }

    @Test
    void getPersonById() {

        Long id = testPerson.getId();

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_READ_ROLE));

        URI uri = UriBuilder.of(PersonEndpoint.PERSON_BY_ID).expand(singletonMap(PersonEndpoint.ID, id));
        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(uri).bearerAuth(refreshToken.getAccessToken()),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$"), isA(Object.class));
        assertThat(ctx.read("$.id"), is(id.intValue()));
        assertThat(ctx.read("$.firstName"), is(testPerson.getFirstName()));
        assertThat(ctx.read("$.lastName"), is(testPerson.getLastName()));
        assertThat(ctx.read("$.dateOfBirth"), isA(Number.class));
    }

    @Test
    void getAllWithDefaultPageAndSize() {

        Pageable pageable = Pageable.from(0);
        Page<Person> persons = personService.findAll(pageable);

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_READ_ROLE));

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(PERSON).bearerAuth(refreshToken.getAccessToken()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, PageableConfiguration.DEFAULT_MAX_PAGE_SIZE, persons);
    }

    @Test
    void getAllWithCustomSize() {

        Pageable pageable = Pageable.from(0, 2);
        Page<Person> persons = personService.findAll(pageable);

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_READ_ROLE));

        URI uri = UriBuilder.of(PERSON)
                .queryParam(PageableConfiguration.DEFAULT_SIZE_PARAMETER, pageable.getSize())
                .build();

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(uri).bearerAuth(refreshToken.getAccessToken()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, pageable.getSize(), persons);
    }

    @Test
    void getAllWithCustomPageAndSize() {

        Pageable pageable = Pageable.from(1, 2);
        Page<Person> persons = personService.findAll(pageable);

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_READ_ROLE));

        URI uri = UriBuilder.of(PERSON)
                .queryParam(PageableConfiguration.DEFAULT_PAGE_PARAMETER, pageable.getNumber())
                .queryParam(PageableConfiguration.DEFAULT_SIZE_PARAMETER, pageable.getSize())
                .build();

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.GET(uri).bearerAuth(refreshToken.getAccessToken()),
                String.class
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, pageable.getSize(), persons);
    }

    @Test
    public void createPersonUnauthorized() throws Exception {

        Person person = createPerson("first", "last");
        person.setMiddleName("middleName");

        String content = json(person);

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.POST(PERSON, content),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, response.getStatus());
    }

    @Test
    public void createPersonForbiddenInvalidScope() throws Exception {

        Person person = createPerson("first", "last");
        person.setMiddleName("middleName");

        String content = json(person);

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_READ_ROLE));

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.POST(PERSON, content).bearerAuth(refreshToken.getAccessToken()),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.FORBIDDEN, response.getStatus());
    }

    @Test
    public void createPerson() throws Exception {

        Person person = createPerson("first", "last");
        person.setMiddleName("middleName");

        String content = json(person);

        BearerAccessRefreshToken refreshToken = loginAndAssert(createCredentials(USER_WITH_WRITE_ROLE));

        HttpResponse<String> response = client.toBlocking().exchange(
                HttpRequest.POST(PERSON, content)
                        .bearerAuth(refreshToken.getAccessToken())
                        .header(PersonEndpoint.HEADER_USER_ID, UUID.randomUUID().toString()),
                Argument.of(String.class),
                Argument.of(String.class)
        );

        assertEquals(HttpStatus.OK, response.getStatus());
        assertTrue(response.getContentType().isPresent());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$"), isA(Object.class));
        assertThat(ctx.read("$.id"), isA(Number.class));
        assertThat(ctx.read("$.firstName"), is(person.getFirstName()));
        assertThat(ctx.read("$.lastName"), is(person.getLastName()));
        assertThat(ctx.read("$.dateOfBirth"), isA(Number.class));
        assertThat(ctx.read("$.dateOfBirth"), is(person.getDateOfBirth().getTime()));
    }

    private void assertPage(ReadContext ctx, Pageable pageable, int defaultMaxPageSize, Page<Person> persons) {

        assertThat(ctx.read("$"), isA(Object.class));
        assertThat(ctx.read("$.pageable.number"), is(pageable.getNumber()));
        assertThat(ctx.read("$.pageable.size"), is(defaultMaxPageSize));
        assertThat(ctx.read("$.content.size()"), is(persons.getContent().size()));
        assertThat(ctx.read("$.totalSize"), is((int) persons.getTotalSize()));
        assertThat(ctx.read("$.totalPages"), is(persons.getTotalPages()));
        assertThat(ctx.read("$.numberOfElements"), is(persons.getNumberOfElements()));
    }

    private Person createPerson(String first, String last) {

        Person person = new Person(first, last);
        person.setDateOfBirth(new Date(timestamp));

        return person;
    }

}
