package com.example.springboot;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

	private final MessageRepository messageRepository;

	public MessageController(MessageRepository messageRepository) {
		this.messageRepository = messageRepository;
	}

	@GetMapping
	public List<Message> list() {
		return messageRepository.findAll();
	}

	@GetMapping("/{id}")
	public Message get(@PathVariable long id) {
		return messageRepository.findById(id).orElseThrow(() -> notFound(id));
	}

	@PostMapping
	@ResponseStatus(HttpStatus.CREATED)
	public Message create(@RequestBody MessageRequest request) {
		return messageRepository.create(validContent(request));
	}

	@PutMapping("/{id}")
	public Message update(@PathVariable long id, @RequestBody MessageRequest request) {
		return messageRepository.update(id, validContent(request))
				.orElseThrow(() -> notFound(id));
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		if (!messageRepository.delete(id)) {
			throw notFound(id);
		}
		return ResponseEntity.noContent().build();
	}

	private static String validContent(MessageRequest request) {
		if (request == null || request.content() == null || request.content().isBlank()) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "content must not be blank");
		}
		return request.content().trim();
	}

	private static ResponseStatusException notFound(long id) {
		return new ResponseStatusException(HttpStatus.NOT_FOUND, "Message " + id + " was not found");
	}

	public record MessageRequest(String content) {
	}
}
