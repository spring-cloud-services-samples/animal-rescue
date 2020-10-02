package io.spring.cloud.samples.animalrescue.backend;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
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
	public String whoami(Principal principal) {
		if (principal == null) {
			return "";
		}
		return principal.getName();
	}

	@GetMapping("/animals")
	public Iterable<Animal> getAllAnimals() {
		LOGGER.info("Received get all animals request");
		return animalRepository.findAll();
	}

	@PostMapping("/animals/{id}/adoption-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public void submitAdoptionRequest(
		Principal principal,
		@PathVariable("id") Long animalId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received submit adoption request from {}", principal.getName());
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		adoptionRequest.setAdopterName(principal.getName());
		animal.getAdoptionRequests().add(adoptionRequest);
		animalRepository.save(animal);
	}

	@PutMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public void editAdoptionRequest(
		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received edit adoption request");
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		AdoptionRequest existing = animal
			.getAdoptionRequests()
			.stream()
			.filter(ar -> ar.getId().equals(adoptionRequestId))
			.findAny()
			.orElseThrow(
				() -> new IllegalArgumentException(String.format("AdoptionRequest with id %s doesn't exist!",
					adoptionRequestId)));


		if (!existing.getAdopterName().equals(principal.getName())) {
			throw new AccessDeniedException(String.format("User %s has cannot edit user %s's adoption request",
				principal.getName(), existing.getAdopterName()));
		}

		existing.setEmail(adoptionRequest.getEmail());
		existing.setNotes(adoptionRequest.getNotes());

		animalRepository.save(animal);
	}

	@DeleteMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public void deleteAdoptionRequest(
		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId
	) {
		LOGGER.info("Received delete adoption request from {}", principal.getName());
		Animal animal = animalRepository
			.findById(animalId)
			.orElseThrow(() ->
				new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId)));

		AdoptionRequest existing = animal
			.getAdoptionRequests()
			.stream()
			.filter(ar -> ar.getId().equals(adoptionRequestId))
			.findAny()
			.orElseThrow(
				() -> new IllegalArgumentException(String.format("AdoptionRequest with id %s doesn't exist!",
					adoptionRequestId)));

		if (!existing.getAdopterName().equals(principal.getName())) {
			throw new AccessDeniedException(String.format("User %s has cannot delete user %s's adoption request",
				principal.getName(), existing.getAdopterName()));
		}

		animal.getAdoptionRequests().remove(existing);
		animalRepository.save(animal);
	}

	@ExceptionHandler({AccessDeniedException.class})
	public ResponseEntity<String> handleAccessDeniedException(Exception e) {
		return new ResponseEntity<>(e.getMessage(), new HttpHeaders(), HttpStatus.FORBIDDEN);
	}

	@ExceptionHandler({IllegalArgumentException.class})
	public ResponseEntity<String> handleIllegalArgumentException(Exception e) {
		return new ResponseEntity<>(e.getMessage(), new HttpHeaders(), HttpStatus.BAD_REQUEST);
	}

}
