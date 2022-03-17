package io.spring.cloud.samples.animalrescue.backend.domain;

import java.security.Principal;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

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
	private final AdoptionRequestRepository adoptionRequestRepository;

	public AnimalController(AnimalRepository animalRepository, AdoptionRequestRepository adoptionRequestRepository) {
		this.animalRepository = animalRepository;
		this.adoptionRequestRepository = adoptionRequestRepository;
	}

	@GetMapping("/animals")
	public Flux<Animal> getAllAnimals() {
		LOGGER.info("Received get all animals request");
		// This code is prioritized to be more readable and maintainable
		// and causes the "N+1 selects problem". Take care of referring to the code.
		return animalRepository.findAll()
				.delayUntil(animal -> adoptionRequestRepository.findByAnimal(animal.getId())
						.collect(Collectors.toSet())
						.doOnNext(animal::setAdoptionRequests));
	}

	@PostMapping("/animals/{id}/adoption-requests")
	@ResponseStatus(HttpStatus.CREATED)
	public Mono<Void> submitAdoptionRequest(
		Principal principal,
		@PathVariable("id") Long animalId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received submit adoption request from {}", principal.getName());
		adoptionRequest.setAnimal(animalId);
		adoptionRequest.setAdopterName(principal.getName());
		return animalRepository
				.findById(animalId)
				.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId))))
				.then(adoptionRequestRepository.save(adoptionRequest))
				.then();
	}

	@PutMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public Mono<Void> editAdoptionRequest(
		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId,
		@RequestBody AdoptionRequest adoptionRequest
	) {
		LOGGER.info("Received edit adoption request");
		return animalRepository
				.findById(animalId)
				.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId))))
				.thenMany(adoptionRequestRepository.findByAnimal(animalId)
						.filter(ar -> ar.getId().equals(adoptionRequestId))
						.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("AdoptionRequest with id %s doesn't exist!",
								adoptionRequestId))))
						.doOnNext(existing -> {
							if (!existing.getAdopterName().equals(principal.getName())) {
								throw new AccessDeniedException(String.format("User %s has cannot delete user %s's adoption request",
										principal.getName(), existing.getAdopterName()));
							}
							existing.setEmail(adoptionRequest.getEmail());
							existing.setNotes(adoptionRequest.getNotes());
						})
						.flatMap(adoptionRequestRepository::save))
				.then();
	}

	@DeleteMapping("/animals/{animalId}/adoption-requests/{adoptionRequestId}")
	public Mono<Void> deleteAdoptionRequest(
		Principal principal,
		@PathVariable("animalId") Long animalId,
		@PathVariable("adoptionRequestId") Long adoptionRequestId
	) {
		LOGGER.info("Received delete adoption request from {}", principal.getName());
		return animalRepository
				.findById(animalId)
				.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("Animal with id %s doesn't exist!", animalId))))
				.thenMany(adoptionRequestRepository.findByAnimal(animalId)
						.filter(ar -> ar.getId().equals(adoptionRequestId))
						.switchIfEmpty(Mono.error(() -> new IllegalArgumentException(String.format("AdoptionRequest with id %s doesn't exist!",
								adoptionRequestId))))
						.doOnNext(existing -> {
							if (!existing.getAdopterName().equals(principal.getName())) {
								throw new AccessDeniedException(String.format("User %s has cannot delete user %s's adoption request",
										principal.getName(), existing.getAdopterName()));
							}
						})
						.flatMap(adoptionRequestRepository::delete))
				.then();
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
