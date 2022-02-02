package com.droidablebee.micronaut.rest.repository;

import com.droidablebee.micronaut.rest.domain.Person;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

@MicronautTest
class PersonRepositoryITest {

    @Inject
    PersonRepository personRepository;

    @PersistenceContext
    EntityManager entityManager;

    @Test
    public void getReferenceUsingEntityManager() {

        Person person = entityManager.getReference(Person.class, Long.MAX_VALUE);
        assertNotNull(person);
        //access to the Entity's reference state should cause javax.persistence.EntityNotFoundException
        assertNotNull(person.getId()); // accessing id won't throw an exception
//javax.persistence.EntityNotFoundException.class is thrown in spring boot
//        assertThrows(javax.persistence.EntityNotFoundException.class, () -> person.getFirstName());
        assertThrows(org.hibernate.ObjectNotFoundException.class, () -> person.getFirstName());
    }

    @Test
    public void findByIdUsingOptional() {

        Optional<Person> optional = personRepository.findById(Long.MAX_VALUE);
        assertNotNull(optional);
        assertFalse(optional.isPresent());
        assertThrows(java.util.NoSuchElementException.class, () -> optional.get());
    }

    @Test
    public void findByIdUsingEntityManager() {

        Person person = entityManager.find(Person.class, Long.MAX_VALUE);
        assertNull(person);
    }
}