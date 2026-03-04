package io.spring.cloud.samples.animalrescue.backend.security;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.config.ApiVersionConfigurer;
import org.springframework.web.reactive.config.WebFluxConfigurer;

@Configuration
public class ApiVersionConfig implements WebFluxConfigurer {

	@Override
	public void configureApiVersioning(ApiVersionConfigurer configurer) {
		configurer
			.useRequestHeader("API-Version")
			.addSupportedVersions("1.0", "2.0")
			.setDefaultVersion("1.0");
	}

}
