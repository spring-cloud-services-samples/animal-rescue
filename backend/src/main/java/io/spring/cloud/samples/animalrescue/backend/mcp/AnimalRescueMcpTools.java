package io.spring.cloud.samples.animalrescue.backend.mcp;

import java.util.List;
import java.util.stream.Collectors;

import io.spring.cloud.samples.animalrescue.backend.domain.AdoptionRequestRepository;
import io.spring.cloud.samples.animalrescue.backend.domain.Animal;
import io.spring.cloud.samples.animalrescue.backend.domain.AnimalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.tool.annotation.Tool;
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

}
