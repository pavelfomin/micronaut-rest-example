package com.droidablebee.micronaut.rest.repository;


import com.droidablebee.micronaut.rest.domain.Person;
import io.micronaut.data.annotation.Query;
import io.micronaut.data.annotation.Repository;
import io.micronaut.data.jpa.annotation.EntityGraph;
import io.micronaut.data.jpa.repository.JpaRepository;
import io.micronaut.data.model.Page;
import io.micronaut.data.model.Pageable;

import javax.validation.constraints.NotNull;
import java.util.Optional;

@Repository
public interface PersonRepository extends JpaRepository<Person, Long> {

    @Override
    //use left join instead of separate sql statements to fetch relationships
    @EntityGraph(attributePaths = {"addresses"})
    Optional<Person> findById(@NotNull Long aLong);

    @Override
    //use left join instead of separate sql statements to fetch relationships
    @Query(value = "select p from Person p left join fetch p.addresses a", countQuery = "select count(p) from Person p left join p.addresses a")
    Page<Person> findAll(Pageable pageable);
}