package com.example.springboot;

import java.time.Instant;

public record Message(Long id, String content, Instant createdAt) {
}
