package org.example.blogdemensajes.model;

import java.time.LocalDateTime;

public class Message {
    private Long id;     // ← ID autoincremental
    private String texto;
    private User user;
    private LocalDateTime createdAt;

    public Message() {}

    public Message(String texto, User user) {
        this.texto = texto;
        this.user = user;
    }

    // --- Getters y Setters ---
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTexto() { return texto; }
    public void setTexto(String texto) { this.texto = texto; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}