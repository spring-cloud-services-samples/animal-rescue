package io.spring.cloud.samples.animalrescue.backend;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;

@Configuration
@Profile("local")
public class LocalRunSecurityConfiguration {

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		// @formatter:off
		return httpSecurity
			.httpBasic().disable()
			.csrf().disable()
			.authorizeExchange()
				.pathMatchers("/whoami").authenticated()
				.anyExchange().permitAll()
				.and()
			.oauth2Login()
				.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("http://localhost:3000/rescue"))
				.and()
			.build();
		// @formatter:on
	}
}
