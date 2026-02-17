package io.spring.cloud.samples.animalrescue.chatserver;

public record ChatMessage(
		String role,    // "user" or "assistant"
		String content
) {
}
