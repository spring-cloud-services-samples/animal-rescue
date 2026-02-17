package io.spring.cloud.samples.animalrescue.chatserver;

import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@CrossOrigin(origins = {"http://localhost:3000", "http://localhost:8080"})
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(@RequestBody ChatRequest request) {
		return chatService.chat(request.message(), request.history());
	}

}
