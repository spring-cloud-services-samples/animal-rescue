package io.spring.cloud.samples.animalrescue.backend.domain;

import reactor.core.publisher.Flux;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AdoptionRequestRepository extends ReactiveCrudRepository<AdoptionRequest, Long> {
	Flux<AdoptionRequest> findByAnimal(Long animal);
}
