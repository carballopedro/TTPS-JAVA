package org.example.blogdemensajes.dao;

import org.example.blogdemensajes.dao.jdbc.MessageJdbcDAO;
import org.example.blogdemensajes.dao.jdbc.UserJdbcDAO;

public final class FactoryDAO {
    private static final MessageDAO MENSAJE_DAO = new MessageJdbcDAO();
    private static final UserDAO USUARIO_DAO = new UserJdbcDAO();

    private FactoryDAO() {}

    public static MessageDAO getMessageDAO() { return MENSAJE_DAO; }
    public static UserDAO getUserDAO() { return USUARIO_DAO; }
}
