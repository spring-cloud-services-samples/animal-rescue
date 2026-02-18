package io.spring.cloud.samples.animalrescue.chatserver.config;

import java.time.Duration;

import org.springframework.boot.http.client.ClientHttpRequestFactoryBuilder;
import org.springframework.boot.http.client.ClientHttpRequestFactorySettings;
import org.springframework.boot.web.client.RestClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatServerConfig {

	@Bean
	RestClientCustomizer restClientCustomizer() {
		return restClientBuilder -> {
			restClientBuilder.requestFactory(ClientHttpRequestFactoryBuilder.jdk()
					.build(ClientHttpRequestFactorySettings.defaults()
							.withConnectTimeout(Duration.ofSeconds(10))
							.withReadTimeout(Duration.ofMinutes(2))));
		};
	}

}
