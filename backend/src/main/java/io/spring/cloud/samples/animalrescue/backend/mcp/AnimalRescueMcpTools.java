package io.spring.cloud.samples.animalrescue.backend.mcp;

import java.util.List;
import java.util.stream.Collectors;

import reactor.core.publisher.Mono;

import io.spring.cloud.samples.animalrescue.backend.domain.AdoptionRequestRepository;
import io.spring.cloud.samples.animalrescue.backend.domain.Animal;
import io.spring.cloud.samples.animalrescue.backend.domain.AnimalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.spring.cloud.samples.animalrescue.backend.domain.AdoptionRequest;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
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

	@Tool(name = "getAvailableAnimals",
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

	@Tool(name = "adoptAnimal",
			description = "Submit an adoption request for a specific animal. " +
					"Requires the animal's ID, the adopter's name, email address, and optional notes. " +
					"Returns a success message or an error message if the animal does not exist.")
	public String adoptAnimal(
			@ToolParam(description = "The numeric ID of the animal to adopt") Long animalId,
			@ToolParam(description = "The name of the adopter") String adopterName,
			@ToolParam(description = "The adopter's contact email address") String email,
			@ToolParam(description = "Optional notes about why the user wants to adopt") String notes
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

}
