package io.spring.cloud.samples.animalrescue.chatserver;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

	private static final Logger LOGGER = LoggerFactory.getLogger(ChatService.class);

	private final ChatClient chatClient;

	public ChatService(ChatClient.Builder chatClientBuilder) {
		this.chatClient = chatClientBuilder
				.defaultSystem("""
						You are a friendly assistant for the Animal Rescue center.
						You help potential adopters find animals that match their preferences.

						When users ask about available animals, use the getAvailableAnimals tool
						to fetch current data. Present results in a friendly, readable format
						using Markdown. Include the animal's name, a brief description, and
						how many pending adoption requests they have.

						If no animals match the user's criteria, say so kindly and suggest
						broadening their search.

						Important guidelines:
						- Only mention animals returned by the tool. Never invent animals.
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

		return chatClient.prompt()
				.messages(messages)
				.user(userMessage)
				.stream()
				.content();
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
