package com.droidablebee.micronaut.rest.endpoint;

import com.droidablebee.micronaut.rest.domain.Person;
import com.droidablebee.micronaut.rest.service.PersonService;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.HttpStatus;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.Post;
import io.micronaut.http.annotation.Put;
import io.micronaut.security.annotation.Secured;
import io.micronaut.security.rules.SecurityRule;
import io.micronaut.validation.Validated;
import jakarta.inject.Inject;

import javax.validation.Valid;

@Controller
@Secured(SecurityRule.IS_AUTHENTICATED)
//@RequestMapping(produces = { MediaType.APPLICATION_JSON_VALUE, MediaType.APPLICATION_XML_VALUE })
@Validated //required for @Valid on method parameters such as @RequestParam, @PathVariable, @RequestHeader
public class PersonEndpoint /*extends BaseEndpoint*/ {

    static final String HEADER_TOKEN = "token";
    static final String HEADER_USER_ID = "userId";

    public static final String PERSON_READ_PERMISSION = "person-read";
    public static final String PERSON_WRITE_PERMISSION = "person-write";

    static final String ID = "id";
    static final String PERSON = "/v1/person";
    static final String PERSON_BY_ID = "/v1/person/{" + ID + "}";

    @Inject
    PersonService personService;

    //todo: swagger

    @Secured(PERSON_READ_PERMISSION)
    @Get(PERSON)
//	@Operation(
//			summary = "Get all persons",
//			description = "Returns first N persons specified by the size parameter with page offset specified by page parameter.")
    public Page<Person> getAll(Pageable pageable) {
//    		/*@Parameter(description = "The size of the page to be returned")*/ @RequestAttribute Integer size,
//    		/*@Parameter(description = "Zero-based page index")*/ @RequestAttribute Integer page) {

//		if (size == null) {
//			size = DEFAULT_PAGE_SIZE;
//		}
//		if (page == null) {
//			page = 0;
//		}
//
//		Pageable pageable = DefaultPageable.of(page, size);
        Page<Person> persons = personService.findAll(pageable);

        return persons;
    }

    @Secured(PERSON_READ_PERMISSION)
    @Get(PERSON_BY_ID)
//	@Operation(
//			summary = "Get person by id",
//			description = "Returns person for id specified.")
//	@ApiResponses(value = {@ApiResponse(responseCode = "404", description = "Person not found") })
    public HttpResponse<Person> get(/*@Parameter(description = "Person id")*/ @PathVariable(ID) Long id) {

        Person person = personService.findOne(id);
        return (person == null ? HttpResponse.status(HttpStatus.NOT_FOUND) : HttpResponse.ok()).body(person);
    }

    @Secured(PERSON_WRITE_PERMISSION)
    @Post(uri = PERSON, consumes = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    @Operation(
//            summary = "Create new or update existing person",
//            description = "Creates new or updates existing person. Returns created/updated person with id.")
    public HttpResponse<Person> add(
            @Valid @Body Person person,
            /*@Valid @Size(max = 40, min = 8, message = "user id size 8-40")*/ @Header(name = HEADER_USER_ID) String userId,
            /*@Valid @Size(max = 40, min = 2, message = "token size 2-40")*/ @Header(name = HEADER_TOKEN, defaultValue = "") String token) {

        person = personService.save(person);
        return HttpResponse.ok().body(person);
    }

    @Secured(PERSON_WRITE_PERMISSION)
    @Put(uri = PERSON_BY_ID, consumes = {MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML})
//    @Operation(
//            summary = "Update existing person",
//            description = "Updates existing person. Returns updated person.")
    public HttpResponse<?> update(
            @PathVariable(ID) Long id,
            @Valid @Body Person person,
            /*@Valid @Size(max = 40, min = 8, message = "user id size 8-40")*/ @Header(name = HEADER_USER_ID) String userId,
            /*@Valid @Size(max = 40, min = 2, message = "token size 2-40")*/ @Header(name = HEADER_TOKEN, defaultValue = "") String token) {

        HttpResponse<?> httpResponse;

        Person found = personService.findOne(id);
        if (found == null) {
            httpResponse = HttpResponse.notFound();
        } else {
            found.setFirstName(person.getFirstName());
            found.setLastName(person.getLastName());
            found.setMiddleName(person.getMiddleName());
            found.setAddresses(person.getAddresses());

            person = personService.update(found);
            httpResponse = HttpResponse.ok().body(person);
        }

        return httpResponse;
    }

//    @InitBinder("person")
//    protected void initBinder(WebDataBinder binder) {
//        binder.addValidators(new PersonValidator());
//    }
}