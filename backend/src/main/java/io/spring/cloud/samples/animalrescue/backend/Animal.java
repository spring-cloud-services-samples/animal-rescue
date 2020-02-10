package io.spring.cloud.samples.animalrescue.backend;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonProperty;

import org.springframework.data.annotation.Id;

public class Animal {

	@Id
	private long id;

	private String name;

	private Date rescueDate;

	private String avatarUrl;

	private String description;

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

	public long getId() {
		return id;
	}

	public void setId(long id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getRescueDate() {
		return rescueDate;
	}

	public void setRescueDate(Date rescueDate) {
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
