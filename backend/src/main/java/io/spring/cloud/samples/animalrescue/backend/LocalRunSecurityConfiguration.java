package io.spring.cloud.samples.animalrescue.backend;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.WebFilterExchange;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.ServerAuthenticationSuccessHandler;
import io.pivotal.cfenv.core.CfEnv;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@Profile("local")
public class LocalRunSecurityConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(LocalRunSecurityConfiguration.class);

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		// @formatter:off
		return httpSecurity
			.httpBasic().disable()
			.csrf().disable()
			.authorizeExchange()
				.pathMatchers("/animals", "/actuators/**").permitAll()
				.and()
			.authorizeExchange()
				.anyExchange().authenticated()
				.and()
			.oauth2ResourceServer()
				.jwt()
				.and()
			.and()
			.oauth2Login()
				.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("http://localhost:3000/rescue/admin"))
				.and()
			.build();
		// @formatter:on
	}
}
