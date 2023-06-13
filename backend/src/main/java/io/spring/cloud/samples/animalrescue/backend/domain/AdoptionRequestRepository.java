package io.spring.cloud.samples.animalrescue.backend.domain;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface AdoptionRequestRepository extends ReactiveCrudRepository<AdoptionRequest, Long> {
	Flux<AdoptionRequest> findByAnimal(Long animal);
}
