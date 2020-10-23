package io.spring.cloud.samples.animalrescue.backend;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.boot.autoconfigure.condition.ConditionalOnCloudPlatform;
import org.springframework.boot.cloud.CloudPlatform;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import io.pivotal.cfenv.core.CfEnv;
import io.pivotal.cfenv.core.CfService;

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
}
