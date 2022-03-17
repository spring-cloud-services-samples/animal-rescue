package io.spring.cloud.samples.animalrescue.backend.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AnimalRepository extends ReactiveCrudRepository<Animal, Long> {
}