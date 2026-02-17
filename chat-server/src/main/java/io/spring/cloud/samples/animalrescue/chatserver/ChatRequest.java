package io.spring.cloud.samples.animalrescue.chatserver;

import java.util.List;

public record ChatRequest(
		String message,
		List<ChatMessage> history
) {
}
