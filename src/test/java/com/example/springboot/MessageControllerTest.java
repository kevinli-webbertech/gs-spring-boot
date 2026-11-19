package com.example.springboot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class MessageControllerTest {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private JdbcTemplate jdbcTemplate;

	@BeforeEach
	void clearMessages() {
		jdbcTemplate.update("DELETE FROM messages");
	}

	@Test
	void createsReadsUpdatesAndDeletesMessage() throws Exception {
		String response = mvc.perform(post("/api/messages")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"content\":\"Running in Kubernetes\"}"))
				.andExpect(status().isCreated())
				.andExpect(jsonPath("$.content").value("Running in Kubernetes"))
				.andReturn().getResponse().getContentAsString();

		JsonNode created = objectMapper.readTree(response);
		long id = created.get("id").asLong();

		mvc.perform(get("/api/messages/{id}", id))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.id").value(id));

		mvc.perform(put("/api/messages/{id}", id)
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"content\":\"Updated from a pod\"}"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.content").value("Updated from a pod"));

		mvc.perform(get("/api/messages"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].id").value(id));

		mvc.perform(delete("/api/messages/{id}", id))
				.andExpect(status().isNoContent());

		mvc.perform(get("/api/messages/{id}", id))
				.andExpect(status().isNotFound());
	}

	@Test
	void rejectsBlankContent() throws Exception {
		mvc.perform(post("/api/messages")
					.contentType(MediaType.APPLICATION_JSON)
					.content("{\"content\":\"   \"}"))
				.andExpect(status().isBadRequest());
	}
}
