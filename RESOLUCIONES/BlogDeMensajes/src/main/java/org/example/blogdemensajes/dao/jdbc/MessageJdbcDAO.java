package org.example.blogdemensajes.dao.jdbc;

import org.example.blogdemensajes.MyDataSource;
import org.example.blogdemensajes.dao.MessageDAO;
import org.example.blogdemensajes.model.Message;
import org.example.blogdemensajes.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageJdbcDAO implements MessageDAO {

    @Override
    public Message findById(Long id) {
        String sql = "SELECT id, texto, username, created_at FROM `Message` WHERE id = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                Message m = new Message();
                m.setId(rs.getLong("id"));
                m.setTexto(rs.getString("texto"));
                m.setUser(new User(rs.getString("username")));
                m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                return m;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findById", e);
        }
    }

    @Override
    public List<Message> findAll() {
        String sql = "SELECT id, texto, username, created_at FROM `Message` ORDER BY id DESC";
        List<Message> out = new ArrayList<>();
        try (Connection con = MyDataSource.getDataSource().getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Message m = new Message();
                m.setId(rs.getLong("id"));
                m.setTexto(rs.getString("texto"));
                m.setUser(new User(rs.getString("username")));
                m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                out.add(m);
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll", e);
        }
    }

    @Override
    public List<Message> findByUser(String username) {
        String sql = "SELECT id, texto, username, created_at FROM `Message` WHERE username = ? ORDER BY id DESC";
        List<Message> out = new ArrayList<>();
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getLong("id"));
                    m.setTexto(rs.getString("texto"));
                    m.setUser(new User(rs.getString("username")));
                    m.setCreatedAt(rs.getTimestamp("created_at").toLocalDateTime());
                    out.add(m);
                }
            }
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findByUser", e);
        }
    }

    @Override
    public void create(Message message) {
        String sql = "INSERT INTO `Message` (texto, username) VALUES (?, ?)";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, message.getTexto());
            ps.setString(2, message.getUser().getUsername());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) message.setId(keys.getLong(1));
            }
        } catch (SQLException e) {
            throw new RuntimeException("create", e);
        }
    }

    @Override
    public void update(Message message) {
        String sql = "UPDATE `Message` SET texto = ?, username = ? WHERE id = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, message.getTexto());
            ps.setString(2, message.getUser().getUsername());
            ps.setLong(3, message.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update", e);
        }
    }

    @Override
    public void delete(Long id) {
        String sql = "DELETE FROM `Message` WHERE id = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setLong(1, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("delete", e);
        }
    }
}