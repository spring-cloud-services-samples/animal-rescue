package io.spring.cloud.samples.animalrescue.chatserver;

import reactor.core.publisher.Flux;

import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ChatController {

	private final ChatService chatService;

	public ChatController(ChatService chatService) {
		this.chatService = chatService;
	}

	@PostMapping(value = "/chat", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public Flux<String> chat(
			@RequestBody ChatRequest request,
			@RequestHeader(name = "X-User-Name", required = false) String claimUsername,
			@CookieValue(name = "SESSION", required = false) String sessionCookie
	) {
		return chatService.chat(request.message(), request.history(), claimUsername, sessionCookie);
	}

}
