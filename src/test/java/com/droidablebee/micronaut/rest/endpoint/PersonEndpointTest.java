package com.droidablebee.micronaut.rest.endpoint;

import com.droidablebee.micronaut.rest.domain.Person;
import com.droidablebee.micronaut.rest.repository.PersonRepository;
import com.droidablebee.micronaut.rest.service.PersonService;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.ReadContext;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.data.runtime.config.DataConfiguration.PageableConfiguration;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.uri.UriBuilder;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import java.net.URI;
import java.util.Date;

import static java.util.Collections.singletonMap;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class PersonEndpointTest /*extends BaseEndpointTest*/ {

    @Inject
    @Client("/")
    HttpClient client;

    @Inject
    EntityManager entityManager;

    @Inject
    PersonService personService;

    @Inject
    PersonRepository personRepository;

    private Person testPerson;
    private long timestamp;

    @BeforeEach
    public void beforeEach() {

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

    //	@Test
//	public void getPersonByIdUnauthorizedNoToken() throws Exception {
//		Long id = testPerson.getId();
//
//		mockMvc.perform(get("/v1/person/{id}", id))
//				.andDo(print())
//				.andExpect(status().isUnauthorized())
//		;
//	}

//	@Test
//	public void getPersonByIdForbiddenInvalidScope() throws Exception {
//		Long id = testPerson.getId();
//
//		mockMvc.perform(get("/v1/person/{id}", id).with(jwt()))
//				.andDo(print())
//				.andExpect(status().isForbidden())
//		;
//	}

    @Test
    public void getPersonById() {

        Long id = testPerson.getId();

        URI uri = UriBuilder.of(PersonEndpoint.GET_BY_ID).expand(singletonMap(PersonEndpoint.ID, id));
        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        ReadContext ctx = JsonPath.parse(response.body());

        assertThat(ctx.read("$"), isA(Object.class));
        assertThat(ctx.read("$.id"), is(id.intValue()));
        assertThat(ctx.read("$.firstName"), is(testPerson.getFirstName()));
        assertThat(ctx.read("$.lastName"), is(testPerson.getLastName()));
        assertThat(ctx.read("$.dateOfBirth"), isA(Number.class));
    }

    @Test
    public void getAllWithDefaultPageAndSize() {

        Pageable pageable = Pageable.from(0);
        Page<Person> persons = personService.findAll(pageable);

        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(PersonEndpoint.GET_ALL), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, PageableConfiguration.DEFAULT_MAX_PAGE_SIZE, persons);
    }

    @Test
    public void getAllWithCustomSize() {

        Pageable pageable = Pageable.from(0, 2);
        Page<Person> persons = personService.findAll(pageable);

        URI uri = UriBuilder.of(PersonEndpoint.GET_ALL)
                .queryParam(PageableConfiguration.DEFAULT_SIZE_PARAMETER, pageable.getSize())
                .build();

        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, pageable.getSize(), persons);
    }

    @Test
    public void getAllWithCustomPageAndSize() {

        Pageable pageable = Pageable.from(1, 2);
        Page<Person> persons = personService.findAll(pageable);

        URI uri = UriBuilder.of(PersonEndpoint.GET_ALL)
                .queryParam(PageableConfiguration.DEFAULT_PAGE_PARAMETER, pageable.getNumber())
                .queryParam(PageableConfiguration.DEFAULT_SIZE_PARAMETER, pageable.getSize())
                .build();

        HttpResponse<String> response = client.toBlocking().exchange(HttpRequest.GET(uri), String.class);

        assertEquals(HttpStatus.OK, response.getStatus());
        assertEquals(MediaType.APPLICATION_JSON_TYPE, response.getContentType().get());

        assertPage(JsonPath.parse(response.body()), pageable, pageable.getSize(), persons);
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
