package io.spring.cloud.samples.animalrescue.backend.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

@Repository
public interface AnimalRepository extends ReactiveCrudRepository<Animal, Long> {
	Mono<Animal> findByNameIgnoreCase(String name);
}