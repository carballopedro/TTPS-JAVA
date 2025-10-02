package org.example.blogdemensajes.dao;


import org.example.blogdemensajes.model.User;

import java.util.List;

public interface UserDAO {
    User findByUserName(String username);
    List<User> findAll();
    void create(User user);
    void update(User user);
    void delete(Long id);
    void deleteByUserName(String username);
}
