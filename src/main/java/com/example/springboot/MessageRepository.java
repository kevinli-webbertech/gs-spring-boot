package com.example.springboot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.core.simple.SimpleJdbcInsert;
import org.springframework.stereotype.Repository;

@Repository
public class MessageRepository {

	private static final RowMapper<Message> MESSAGE_ROW_MAPPER = MessageRepository::mapMessage;

	private final JdbcTemplate jdbcTemplate;
	private final SimpleJdbcInsert messageInsert;

	public MessageRepository(JdbcTemplate jdbcTemplate) {
		this.jdbcTemplate = jdbcTemplate;
		this.messageInsert = new SimpleJdbcInsert(jdbcTemplate)
				.withTableName("messages")
				.usingColumns("content")
				.usingGeneratedKeyColumns("id");
	}

	public List<Message> findAll() {
		return jdbcTemplate.query(
				"SELECT id, content, created_at FROM messages ORDER BY id",
				MESSAGE_ROW_MAPPER);
	}

	public Optional<Message> findById(long id) {
		return jdbcTemplate.query(
				"SELECT id, content, created_at FROM messages WHERE id = ?",
				MESSAGE_ROW_MAPPER,
				id).stream().findFirst();
	}

	public Message create(String content) {
		Number id = messageInsert.executeAndReturnKey(Map.of("content", content));
		return findById(id.longValue())
				.orElseThrow(() -> new IllegalStateException("Created message could not be loaded"));
	}

	public Optional<Message> update(long id, String content) {
		int updatedRows = jdbcTemplate.update(
				"UPDATE messages SET content = ? WHERE id = ?",
				content,
				id);
		return updatedRows == 0 ? Optional.empty() : findById(id);
	}

	public boolean delete(long id) {
		return jdbcTemplate.update("DELETE FROM messages WHERE id = ?", id) > 0;
	}

	private static Message mapMessage(ResultSet resultSet, int rowNumber) throws SQLException {
		Timestamp createdAt = resultSet.getTimestamp("created_at");
		return new Message(
				resultSet.getLong("id"),
				resultSet.getString("content"),
				createdAt.toInstant());
	}
}
