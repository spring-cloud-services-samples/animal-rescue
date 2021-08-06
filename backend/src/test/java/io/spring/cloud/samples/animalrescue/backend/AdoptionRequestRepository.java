package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface AdoptionRequestRepository extends ReactiveCrudRepository<AdoptionRequest, Long> {
}
