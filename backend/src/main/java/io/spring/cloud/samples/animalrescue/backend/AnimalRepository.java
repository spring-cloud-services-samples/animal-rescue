package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AnimalRepository extends ReactiveCrudRepository<Animal, Long> {
}