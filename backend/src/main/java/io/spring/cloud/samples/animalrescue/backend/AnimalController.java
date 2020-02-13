package io.spring.cloud.samples.animalrescue.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
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
//		Principal principal,
		@PathVariable("id") Long animalId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received submit adoption request");
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		adoptionRequest.setAdopterName("dummy");
		animal.getAdoptionRequests().add(adoptionRequest);
		animalRepository.save(animal);
	}

	@PutMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public void editAdoptionRequest(
//		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received edit adoption request");
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		animal
			.getAdoptionRequests()
			.stream()
			.filter(existing -> existing.getId().equals(adoptionRequestId))
			.findAny()
			.ifPresent(existing -> {
				existing.setEmail(adoptionRequest.getEmail());
				existing.setNotes(adoptionRequest.getNotes());
			});

		animalRepository.save(animal);
	}

	@DeleteMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public void deleteAdoptionRequest(
//		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId
	) {
		LOGGER.info("Received edit adoption request");
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		animal
			.getAdoptionRequests()
			.removeIf(existing -> existing.getId().equals(adoptionRequestId));

		animalRepository.save(animal);
	}
}
