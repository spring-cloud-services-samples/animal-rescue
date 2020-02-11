package io.spring.cloud.samples.animalrescue.backend;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AnimalController {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimalController.class);

	private final AnimalRepository animalRepository;

	public AnimalController(AnimalRepository animalRepository) {
		this.animalRepository = animalRepository;
	}

	@GetMapping("/whoami")
	public Authentication whoami(Authentication authentication) {
		return authentication;
	}

	@GetMapping("/animals")
	public Iterable<Animal> getAllAnimals() {
		LOGGER.info("Received get all animals request");
		return animalRepository.findAll();
	}

	@PostMapping("/animals/{id}/adoption-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public void submitAdoptionRequest(
		@PathVariable("id") Long animalId,
		@RequestBody AdoptionRequest adoptionRequest,
		Principal principal
	) {
		LOGGER.info("Received submit adoption request from {}", principal);
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		adoptionRequest.setAdopterName(principal.getName());
		animal.getAdoptionRequests().add(adoptionRequest);
		animalRepository.save(animal);
	}
}
