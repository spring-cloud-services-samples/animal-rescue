package io.spring.cloud.samples.animalrescue.backend;

public class AdoptionRequest {
	private String adopterName;
	private String notes;

	@Override
	public String toString() {
		return "AdoptionRequest{" +
			", adopterName='" + adopterName + '\'' +
			", notes='" + notes + '\'' +
			'}';
	}

	public String getAdopterName() {
		return adopterName;
	}

	public void setAdopterName(String adopterName) {
		this.adopterName = adopterName;
	}

	public String getNotes() {
		return notes;
	}

	public void setNotes(String notes) {
		this.notes = notes;
	}

}
