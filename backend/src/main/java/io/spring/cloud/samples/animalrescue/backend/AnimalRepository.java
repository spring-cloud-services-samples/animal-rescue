package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.data.repository.CrudRepository;

public interface AnimalRepository extends CrudRepository<Animal, Long> {
}
