package io.spring.cloud.samples.animalrescue.backend;

import io.spring.cloud.samples.animalrescue.backend.mcp.AnimalRescueMcpTools;

import org.springframework.ai.tool.ToolCallbackProvider;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class AnimalRescueBackendApplication {

	public static void main(String[] args) {
		SpringApplication.run(AnimalRescueBackendApplication.class, args);
	}

	@Bean
	ToolCallbackProvider animalRescueTools(AnimalRescueMcpTools mcpTools) {
		return MethodToolCallbackProvider.builder()
				.toolObjects(mcpTools)
				.build();
	}

}
