package io.spring.cloud.samples.animalrescue.backend;

import java.util.List;

import org.springframework.data.repository.CrudRepository;

public interface AdoptionRequestRepository extends CrudRepository<AdoptionRequest, Long> {
	List<AdoptionRequest> findByAnimal(Long animal);
}
