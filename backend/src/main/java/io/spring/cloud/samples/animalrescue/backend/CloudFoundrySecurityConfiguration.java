package io.spring.cloud.samples.animalrescue.backend;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.converter.Converter;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@ConditionalOnCloudPlatform(CloudPlatform.CLOUD_FOUNDRY)
public class CloudFoundrySecurityConfiguration {

	private static final Logger LOG = LoggerFactory.getLogger(CloudFoundrySecurityConfiguration.class);

	@Bean
	CfEnv cfEnv() {
		return new CfEnv();
	}

	@Bean
	public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity httpSecurity, CfEnv cfEnv) {
		List<CfService> services = cfEnv.findServicesByLabel("p.gateway");
		if (services.isEmpty()) return httpSecurity.build();

		String authDomain = cfEnv.findCredentialsByLabel("p.gateway").getString("auth_domain");
		if (authDomain != null) {
			LOG.info("Found SSO auth_domain {}, configuring Resource Server support", authDomain);
			httpSecurity.oauth2ResourceServer()
			            .jwt()
			            .jwkSetUri(authDomain + "/token_keys")
						.jwtAuthenticationConverter(new ReactiveJwtAuthenticationConverterAdapter(new UserNameJwtAuthenticationConverter()));
		}

		return httpSecurity.build();
	}

	static private class UserNameJwtAuthenticationConverter implements Converter<Jwt, AbstractAuthenticationToken> {

		private Converter<Jwt, Collection<GrantedAuthority>> jwtGrantedAuthoritiesConverter
			= new JwtGrantedAuthoritiesConverter();

		@Override
		public AbstractAuthenticationToken convert(Jwt jwt) {
			Collection<GrantedAuthority> authorities = this.jwtGrantedAuthoritiesConverter.convert(jwt);
			return new JwtAuthenticationToken(jwt, authorities, getUserName(jwt));
		}

		private String getUserName(Jwt jwt) {
			return jwt.containsClaim("user_name") ? jwt.getClaimAsString("user_name") : jwt.getSubject();
		}
	}
}
