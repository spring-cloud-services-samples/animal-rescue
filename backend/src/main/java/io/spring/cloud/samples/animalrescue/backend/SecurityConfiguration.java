package io.spring.cloud.samples.animalrescue.backend;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;
import org.springframework.security.web.server.util.matcher.PathPatternParserServerWebExchangeMatcher;
import org.springframework.web.util.pattern.PathPattern;

@Configuration
public class SecurityConfiguration {

	@Bean
	PasswordEncoder passwordEncoder() {
		return new BCryptPasswordEncoder();
	}

	@Bean
	ReactiveUserDetailsService reactiveUserDetailsService(PasswordEncoder passwordEncoder) {
		return new MapReactiveUserDetailsService(
			User.withUsername("alice").password(passwordEncoder.encode("test")).roles("USER").build(),
			User.withUsername("bob").password(passwordEncoder.encode("test")).roles("USER").build()
		);
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity,
	                                                     @Value("${redirect-uri}") String redirectUri) {
		// @formatter:off
		RedirectServerLogoutSuccessHandler logoutHandler = new RedirectServerLogoutSuccessHandler();
		logoutHandler.setLogoutSuccessUrl(URI.create(redirectUri));
		return httpSecurity
			.httpBasic().disable()
			.csrf().disable()
			.formLogin()
				.loginPage("/login")
				.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler(redirectUri))
				.and()
			.logout()
				.requiresLogout(new PathPatternParserServerWebExchangeMatcher("/logout", HttpMethod.GET))
				.logoutSuccessHandler(logoutHandler)
				.and()
			.authorizeExchange()
				.pathMatchers("/whoami").authenticated()
				.anyExchange().permitAll()
			.and()
			.build();
		// @formatter:on
	}
}
