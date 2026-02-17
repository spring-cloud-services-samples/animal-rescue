package io.spring.cloud.samples.animalrescue.chatserver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class ChatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

	private final ChatClient chatClient;
	private final WebClient backendClient;

	public ChatService(ChatClient.Builder chatClientBuilder,
			@Value("${animal-rescue.backend-url:http://localhost:8080}") String backendUrl) {
		this.backendClient = WebClient.create(backendUrl);
		this.chatClient = chatClientBuilder
				.defaultSystem("""
						You are a friendly assistant for the Animal Rescue center.
						You help potential adopters find animals that match their preferences.

						Present results in a friendly, readable format using Markdown.
						Include the animal's name, a brief description, and how many
						pending adoption requests they have.

						If no animals match the user's criteria, say so kindly and suggest
						broadening their search.

						Important guidelines:
						- Only mention animals from the provided data. Never invent animals.
						- The animal descriptions contain hints about personality, energy level,
						  and temperament. Use these to match user preferences.
						- The avatarUrl often contains breed information in the URL path.
						- Be conversational and warm. You represent a rescue center that cares
						  about finding the right match between adopters and animals.
						""")
				.build();
	}

	public Flux<String> chat(String userMessage, List<ChatMessage> history) {
		LOGGER.info("Processing chat message: {}", userMessage);

		List<Message> messages = buildMessages(history);

		boolean needsAnimalData = looksLikeAnimalQuery(userMessage);

		if (needsAnimalData) {
			return fetchAnimalData()
					.flatMap(animalJson -> {
						String enrichedMessage = userMessage + "\n\n"
								+ "Here is the current animal data from our rescue center "
								+ "(use this to answer the question):\n" + animalJson;
						return Mono.fromCallable(() -> chatClient.prompt()
								.messages(messages)
								.user(enrichedMessage)
								.call()
								.content())
								.subscribeOn(Schedulers.boundedElastic());
					})
					.flux();
		}

		return Mono.fromCallable(() -> chatClient.prompt()
				.messages(messages)
				.user(userMessage)
				.call()
				.content())
				.subscribeOn(Schedulers.boundedElastic())
				.flux();
	}

	private boolean looksLikeAnimalQuery(String message) {
		String lower = message.toLowerCase();
		return lower.contains("animal") || lower.contains("adopt") || lower.contains("pet")
				|| lower.contains("dog") || lower.contains("cat") || lower.contains("available")
				|| lower.contains("breed") || lower.contains("rescue") || lower.contains("kitten")
				|| lower.contains("puppy");
	}

	private Mono<String> fetchAnimalData() {
		return backendClient.get()
				.uri("/animals")
				.retrieve()
				.bodyToMono(String.class)
				.doOnNext(data -> LOGGER.debug("Fetched animal data: {} chars", data.length()))
				.onErrorResume(e -> {
					LOGGER.error("Failed to fetch animal data from backend", e);
					return Mono.just("[]");
				});
	}

	private List<Message> buildMessages(List<ChatMessage> history) {
		if (history == null || history.isEmpty()) {
			return List.of();
		}

		List<Message> messages = new ArrayList<>();
		for (ChatMessage msg : history) {
			if ("user".equals(msg.role())) {
				messages.add(new UserMessage(msg.content()));
			}
			else if ("assistant".equals(msg.role())) {
				messages.add(new AssistantMessage(msg.content()));
			}
		}
		return messages;
	}

}
