package io.spring.cloud.samples.animalrescue.backend.domain;

import java.util.List;

public class AnimalPage {

	private List<Animal> animals;

	private long totalCount;

	private boolean hasMore;

	public AnimalPage(List<Animal> animals, long totalCount, boolean hasMore) {
		this.animals = animals;
		this.totalCount = totalCount;
		this.hasMore = hasMore;
	}

	public List<Animal> getAnimals() {
		return animals;
	}

	public void setAnimals(List<Animal> animals) {
		this.animals = animals;
	}

	public long getTotalCount() {
		return totalCount;
	}

	public void setTotalCount(long totalCount) {
		this.totalCount = totalCount;
	}

	public boolean isHasMore() {
		return hasMore;
	}

	public void setHasMore(boolean hasMore) {
		this.hasMore = hasMore;
	}

}
