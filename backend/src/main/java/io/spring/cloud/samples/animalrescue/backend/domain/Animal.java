package io.spring.cloud.samples.animalrescue.backend.domain;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;

public class Animal {

	@Id
	private Long id;

	private String name;

	private LocalDate rescueDate;

	private String avatarUrl;

	private String description;

	@Transient
	private Set<AdoptionRequest> adoptionRequests = new HashSet<>();

	@Override
	public String toString() {
		return "Animal{" +
			"id=" + id +
			", name='" + name + '\'' +
			", rescueDate=" + rescueDate +
			", avatarUrl='" + avatarUrl + '\'' +
			", description='" + description + '\'' +
			'}';
	}

	public Long getId() {
		return id;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public LocalDate getRescueDate() {
		return rescueDate;
	}

	public void setRescueDate(LocalDate rescueDate) {
		this.rescueDate = rescueDate;
	}

	public String getAvatarUrl() {
		return avatarUrl;
	}

	public void setAvatarUrl(String avatarUrl) {
		this.avatarUrl = avatarUrl;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public Set<AdoptionRequest> getAdoptionRequests() {
		return adoptionRequests;
	}

	public void setAdoptionRequests(Set<AdoptionRequest> adoptionRequests) {
		this.adoptionRequests = adoptionRequests;
	}

}
