package io.spring.cloud.samples.animalrescue.backend;


import java.time.LocalDate;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.function.Function;

import io.r2dbc.spi.Row;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.r2dbc.core.DatabaseClient.GenericExecuteSpec;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;

@Repository
public class AnimalRepository {
	private final DatabaseClient databaseClient;

	public AnimalRepository(DatabaseClient databaseClient) {
		this.databaseClient = databaseClient;
	}

	public Flux<Animal> findAll() {
		return this.databaseClient.sql("select a.id, a.name, a.rescue_date, a.avatar_url, a.description, r.id as rid, r.adopter_name, r.email, r.notes from animal a left join adoption_request r on a.id = r.animal")
				.map(Function.identity())
				.all()
				.collectMultimap(row -> row.get("id", Long.class), Function.identity(), LinkedHashMap::new)
				.flatMapIterable(Map::entrySet)
				.mapNotNull(entry -> mapRows(entry.getValue()));
	}

	public Mono<Animal> findById(long animalId) {
		return this.databaseClient.sql("select a.id, a.name, a.rescue_date, a.avatar_url, a.description, r.id as rid, r.adopter_name, r.email, r.notes from animal a left join adoption_request r on a.id = r.animal where a.id = :id")
				.bind("id", animalId)
				.map(Function.identity())
				.all()
				.collectList()
				.mapNotNull(this::mapRows);
	}

	Animal mapRows(Collection<Row> rows) {
		if (rows.isEmpty()) {
			return null;
		}
		Animal animal = new Animal();
		for (Row row : rows) {
			if (animal.getName() == null) {
				animal.setId(row.get("id", Long.class));
				animal.setName(row.get("name", String.class));
				animal.setRescueDate(row.get("rescue_date", LocalDate.class));
				animal.setAvatarUrl(row.get("avatar_url", String.class));
				animal.setDescription(row.get("description", String.class));
				animal.setAdoptionRequests(new LinkedHashSet<>());
			}
			Long requestId = row.get("rid", Long.class);
			if (requestId != null) {
				AdoptionRequest request = new AdoptionRequest();
				request.setId(requestId);
				request.setAdopterName(row.get("adopter_name", String.class));
				request.setEmail(row.get("email", String.class));
				request.setNotes(row.get("notes", String.class));
				animal.getAdoptionRequests().add(request);
			}
		}
		return animal;
	}

	@Transactional
	public Mono<Void> save(Animal animal) {
		Long animalId = animal.getId();
		if (animalId == null) {
			return this.databaseClient.sql("insert into animal ( name, rescue_date, avatar_url, description) values (:name, :rescue_date, :avatar_url, :description)")
					.filter(statement -> statement.returnGeneratedValues("id"))
					.bind("name", animal.getName())
					.bind("rescue_date", animal.getRescueDate())
					.bind("avatar_url", animal.getAvatarUrl())
					.bind("description", animal.getDescription())
					.map(row -> row.get("id", Long.class))
					.one()
					.flatMapMany(generatedId -> this.insertAdoptionRequests(generatedId, animal.getAdoptionRequests()))
					.then();
		}
		return this.databaseClient.sql("update animal set name = :name, rescue_date = :rescue_date, avatar_url = :avatar_url, description = :description where id = :id")
				.bind("id", animalId)
				.bind("name", animal.getName())
				.bind("rescue_date", animal.getRescueDate())
				.bind("avatar_url", animal.getAvatarUrl())
				.bind("description", animal.getDescription())
				.fetch()
				.rowsUpdated()
				.then(this.databaseClient.sql("delete from adoption_request where animal = :animal")
						.bind("animal", animalId)
						.fetch()
						.rowsUpdated()
						.thenMany(this.insertAdoptionRequests(animalId, animal.getAdoptionRequests()))
						.then());
	}

	Flux<Integer> insertAdoptionRequests(Long animalId, Collection<AdoptionRequest> adoptionRequests) {
		if (CollectionUtils.isEmpty(adoptionRequests)) {
			return Flux.empty();
		}
		return Flux.fromIterable(adoptionRequests)
				.flatMap(request -> {
					GenericExecuteSpec spec = this.databaseClient.sql("insert into adoption_request (id, animal, adopter_name, email, notes) values(:id, :animal, :adopter_name, :email, :notes)")
							.bind("animal", animalId)
							.bind("adopter_name", request.getAdopterName())
							.bind("email", request.getEmail())
							.bind("notes", request.getNotes());
					Long requestId = request.getId();
					return (requestId == null ? spec.bindNull("id", Long.class) : spec.bind("id", requestId))
							.fetch()
							.rowsUpdated();
				});
	}
}