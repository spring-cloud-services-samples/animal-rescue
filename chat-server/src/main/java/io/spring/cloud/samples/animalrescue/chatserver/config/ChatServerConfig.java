package io.spring.cloud.samples.animalrescue.chatserver.config;

import org.springframework.context.annotation.Configuration;

@Configuration
public class ChatServerConfig {

	// Spring AI auto-configuration handles:
	// - ChatClient.Builder (from spring-ai-starter-model-openai)
	// - MCP client connections (from spring-ai-starter-mcp-client)
	// - ToolCallbackProvider (auto-discovered from MCP server tools)
	//
	// No explicit bean definitions needed unless customization is required.

}
