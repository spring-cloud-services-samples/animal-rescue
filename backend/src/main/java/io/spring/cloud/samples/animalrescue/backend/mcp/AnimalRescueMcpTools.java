package io.spring.cloud.samples.animalrescue.backend.mcp;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import io.spring.cloud.samples.animalrescue.backend.domain.AdoptionRequestRepository;
import io.spring.cloud.samples.animalrescue.backend.domain.Animal;
import io.spring.cloud.samples.animalrescue.backend.domain.AnimalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.cloud.samples.animalrescue.backend.domain.AdoptionRequest;

import org.springaicommunity.mcp.annotation.McpTool;
import org.springaicommunity.mcp.annotation.McpToolParam;
import org.springframework.stereotype.Component;

@Component
public class AnimalRescueMcpTools {

	private static final Logger LOGGER = LoggerFactory.getLogger(AnimalRescueMcpTools.class);

	private final AnimalRepository animalRepository;
	private final AdoptionRequestRepository adoptionRequestRepository;

	public AnimalRescueMcpTools(AnimalRepository animalRepository,
			AdoptionRequestRepository adoptionRequestRepository) {
		this.animalRepository = animalRepository;
		this.adoptionRequestRepository = adoptionRequestRepository;
	}

	@McpTool(name = "getAvailableAnimals",
			description = "Get all animals available for adoption at the rescue center. " +
					"Returns a list of animals with their name, description, rescue date, " +
					"avatar URL, and current adoption requests. Use this tool when the user " +
					"asks about available animals, specific breeds, or animal characteristics.")
	public List<Animal> getAvailableAnimals() {
		LOGGER.info("MCP tool invoked: getAvailableAnimals");
		return animalRepository.findAll()
				.delayUntil(animal -> adoptionRequestRepository.findByAnimal(animal.getId())
						.collect(Collectors.toSet())
						.doOnNext(animal::setAdoptionRequests))
				.collectList()
				.block();
	}

	@McpTool(name = "adoptAnimal",
			description = "Submit an adoption request for a specific animal. " +
					"Requires the animal's ID, the adopter's name, email address, and optional notes. " +
					"Returns a success message or an error message if the animal does not exist.")
	public String adoptAnimal(
			@McpToolParam(description = "The numeric ID of the animal to adopt") Long animalId,
			@McpToolParam(description = "The name of the adopter") String adopterName,
			@McpToolParam(description = "The adopter's contact email address") String email,
			@McpToolParam(description = "Optional notes about why the user wants to adopt") String notes
	) {
		LOGGER.info("MCP tool invoked: adoptAnimal(animalId={}, adopterName={}, email={})", animalId, adopterName, email);
		return animalRepository.findById(animalId)
				.flatMap(animal -> {
					AdoptionRequest request = new AdoptionRequest();
					request.setAdopterName(adopterName);
					request.setEmail(email);
					request.setNotes(notes != null ? notes : "");
					request.setAnimal(animalId);
					return adoptionRequestRepository.save(request)
							.map(saved -> "Adoption request submitted successfully for " + animal.getName() + "!");
				})
				.switchIfEmpty(Mono.just("Error: Animal with id " + animalId + " doesn't exist!"))
				.block();
	}

	@McpTool(name = "getPendingAdopters",
			description = "Get the list of pending adopters for a specific animal. " +
					"Requires the animal's name. Returns adopter names, emails, and notes. " +
					"This tool should only be used for authenticated users.")
	public List<AdoptionRequest> getPendingAdopters(
			@McpToolParam(description = "The name of the animal to look up pending adopters for") String animalName
	) {
		LOGGER.info("MCP tool invoked: getPendingAdopters(animalName={})", animalName);
		return animalRepository.findByNameIgnoreCase(animalName)
				.flatMap(animal -> adoptionRequestRepository.findByAnimal(animal.getId())
						.collectList())
				.defaultIfEmpty(Collections.emptyList())
				.block();
	}

}
