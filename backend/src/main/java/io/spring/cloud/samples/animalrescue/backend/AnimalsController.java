package io.spring.cloud.samples.animalrescue.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnimalsController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimalsController.class);

	private final AnimalRepository animalRepository;

	public AnimalsController(AnimalRepository animalRepository) {
		this.animalRepository = animalRepository;
	}

	@GetMapping("/animals")
	public Iterable<Animal> getAllAnimals() {
		LOGGER.info("Received get all animals request");
		return animalRepository.findAll();
	}

}
