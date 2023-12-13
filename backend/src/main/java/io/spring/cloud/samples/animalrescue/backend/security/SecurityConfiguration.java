package io.spring.cloud.samples.animalrescue.backend.security;

import java.net.URI;

import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotatedTypeMetadata;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.ReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.security.web.server.authentication.RedirectServerAuthenticationSuccessHandler;
import org.springframework.security.web.server.authentication.logout.RedirectServerLogoutSuccessHandler;

@Configuration
@Conditional(SecurityConfiguration.NotOnCloudCondition.class)
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
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity) {
		// @formatter:off
		RedirectServerLogoutSuccessHandler logoutHandler = new RedirectServerLogoutSuccessHandler();
		logoutHandler.setLogoutSuccessUrl(URI.create("http://localhost:3000/rescue"));
		return httpSecurity
			.httpBasic(httpBasicSpec -> {
				httpBasicSpec.disable();
			})
			.formLogin(formLoginSpec -> {
				formLoginSpec.authenticationSuccessHandler(new RedirectServerAuthenticationSuccessHandler("http://localhost:3000/rescue"));
			})
			.logout(logoutSpec -> {
				logoutSpec.logoutSuccessHandler(logoutHandler);
			})
			.csrf(csrfSpec -> {
				csrfSpec.disable();
			})
			.authorizeExchange(authorizeExchangeSpec -> {
				authorizeExchangeSpec
					.pathMatchers("/whoami").authenticated()
					.anyExchange().permitAll();
			})
			.build();
		// @formatter:on
	}

	static class NotOnCloudCondition implements Condition {

		@Override
		public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
			var cloudPlatform = CloudPlatform.getActive(context.getEnvironment());
			return cloudPlatform == null || cloudPlatform == CloudPlatform.NONE;
		}
	}

}
