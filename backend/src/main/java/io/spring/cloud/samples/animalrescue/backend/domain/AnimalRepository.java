package io.spring.cloud.samples.animalrescue.backend.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AnimalRepository extends ReactiveCrudRepository<Animal, Long> {
}