package com.droidablebee.micronaut.rest.service;

import com.droidablebee.micronaut.rest.domain.Person;
import com.droidablebee.micronaut.rest.repository.PersonRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;
import io.micronaut.transaction.annotation.ReadOnly;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import javax.transaction.Transactional;
import java.util.Optional;

@Singleton
@Transactional
public class PersonService {

	@Inject
	PersonRepository repository;

	@ReadOnly
	public Page<Person> findAll(Pageable pageable) {
		
		return repository.findAll(pageable);
	}
	
	@ReadOnly
	public Person findOne(Long id) {

		Optional<Person> person = repository.findById(id);
		return person.isPresent() ? person.get() : null;
	}
	
	public Person save(Person person) {
		
		return repository.saveAndFlush(person);
	}
}
