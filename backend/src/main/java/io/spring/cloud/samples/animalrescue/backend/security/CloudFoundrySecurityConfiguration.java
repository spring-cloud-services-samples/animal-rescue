package io.spring.cloud.samples.animalrescue.backend.security;

import java.security.Principal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.web.server.SecurityWebFiltersOrder;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.WebFilter;

/**
 * Cloud security configuration that relies on API Gateway's ClaimHeader filter
 * to forward the authenticated username via the {@code X-User-Name} HTTP header.
 * This replaces the previous TokenRelay + JWT resource server approach.
 */
@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundrySecurityConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundrySecurityConfiguration.class);

	static final String USER_NAME_HEADER = "X-User-Name";
	static final String USER_SUB_HEADER = "X-User-Sub";

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		return httpSecurity
			.csrf(csrfSpec -> csrfSpec.disable())
			.httpBasic(httpBasicSpec -> httpBasicSpec.disable())
			.formLogin(formLoginSpec -> formLoginSpec.disable())
			.addFilterBefore(claimHeaderAuthenticationFilter(), SecurityWebFiltersOrder.AUTHENTICATION)
			.authorizeExchange(authorizeExchangeSpec -> {
				authorizeExchangeSpec
					.pathMatchers("/whoami").authenticated()
					.anyExchange().permitAll();
			})
			.build();
	}

	/**
	 * Reads the {@code X-User-Name} header set by the API Gateway's ClaimHeader
	 * filter and populates the security context with a {@link Principal} so that
	 * downstream controllers (e.g. {@code AnimalController}) can use
	 * {@code Principal.getName()} unchanged.
	 *
	 * Falls back to {@code X-User-Sub} if {@code X-User-Name} is absent.
	 */
	private WebFilter claimHeaderAuthenticationFilter() {
		return (exchange, chain) -> {
			String username = exchange.getRequest().getHeaders().getFirst(USER_NAME_HEADER);
			if (username == null || username.isBlank()) {
				username = exchange.getRequest().getHeaders().getFirst(USER_SUB_HEADER);
			}
			if (username != null && !username.isBlank()) {
				LOG.debug("Authenticated via ClaimHeader: {}", username);
				Authentication auth = new UsernamePasswordAuthenticationToken(
					username, null, AuthorityUtils.createAuthorityList("ROLE_USER"));
				return chain.filter(exchange)
					.contextWrite(ReactiveSecurityContextHolder.withAuthentication(auth));
			}
			return chain.filter(exchange);
		};
	}
}
