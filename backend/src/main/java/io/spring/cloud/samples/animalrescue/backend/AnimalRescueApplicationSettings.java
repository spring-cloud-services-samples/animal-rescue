package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "animal-rescue")
public class AnimalRescueApplicationSettings {
	private int adoptionRequestLimit;

	public int getAdoptionRequestLimit() {
		return adoptionRequestLimit;
	}

	public void setAdoptionRequestLimit(int adoptionRequestLimit) {
		this.adoptionRequestLimit = adoptionRequestLimit;
	}
}
