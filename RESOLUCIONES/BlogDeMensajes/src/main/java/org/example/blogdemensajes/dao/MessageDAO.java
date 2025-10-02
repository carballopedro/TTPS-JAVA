package org.example.blogdemensajes.dao;

import org.example.blogdemensajes.model.Message;

import java.util.List;

public interface MessageDAO {
    Message findById(Long id);
    List<Message> findAll();
    List<Message> findByUser(String username);
    void create(Message message);
    void update(Message message);
    void delete(Long id);
}