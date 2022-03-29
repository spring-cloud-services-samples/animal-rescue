package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({AnimalRescueApplicationSettings.class})
public class AnimalRescueBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnimalRescueBackendApplication.class, args);
	}

}
