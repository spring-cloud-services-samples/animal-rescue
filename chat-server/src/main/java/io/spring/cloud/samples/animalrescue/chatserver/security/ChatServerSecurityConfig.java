package io.spring.cloud.samples.animalrescue.chatserver.security;

import java.util.List;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

@Configuration
@Conditional(ChatServerSecurityConfig.NotOnCloudCondition.class)
public class ChatServerSecurityConfig {

	@Bean
	public CorsWebFilter corsFilter() {
		var config = new CorsConfiguration();
		config.setAllowedOrigins(List.of("http://localhost:3000"));
		config.setAllowedMethods(List.of("POST", "OPTIONS"));
		config.setAllowCredentials(true);
		config.setAllowedHeaders(List.of("*"));

		var source = new UrlBasedCorsConfigurationSource();
		source.registerCorsConfiguration("/**", config);
		return new CorsWebFilter(source);
	}

	static class NotOnCloudCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			var cloudPlatform = CloudPlatform.getActive(context.getEnvironment());
			return cloudPlatform == null || cloudPlatform == CloudPlatform.NONE;
		}
	}

}
