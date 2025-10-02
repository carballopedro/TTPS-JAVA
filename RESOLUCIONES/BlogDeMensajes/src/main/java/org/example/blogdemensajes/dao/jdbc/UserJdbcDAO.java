package org.example.blogdemensajes.dao.jdbc;

import org.example.blogdemensajes.MyDataSource;
import org.example.blogdemensajes.dao.UserDAO;
import org.example.blogdemensajes.model.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserJdbcDAO implements UserDAO {

    @Override
    public User findByUserName(String username) {
        String sql = "SELECT username FROM `User` WHERE username = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? new User(rs.getString("username")) : null;
            }
        } catch (SQLException e) {
            throw new RuntimeException("findByUserName", e);
        }
    }

    @Override
    public List<User> findAll() {
        String sql = "SELECT username FROM `User` ORDER BY username";
        List<User> out = new ArrayList<>();
        try (Connection con = MyDataSource.getDataSource().getConnection();
             Statement st = con.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) out.add(new User(rs.getString("username")));
            return out;
        } catch (SQLException e) {
            throw new RuntimeException("findAll", e);
        }
    }

    @Override
    public void create(User user) {
        String sql = "INSERT INTO `User` (username) VALUES (?)";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("create", e);
        }
    }

    @Override
    public void update(User user) {
        // Sin “oldUsername” no hay cambio real. Se deja por compatibilidad.
        String sql = "UPDATE `User` SET username = ? WHERE username = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getUsername());
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("update", e);
        }
    }

    @Override
    public void delete(Long id) {
        throw new UnsupportedOperationException("Borrado por ID no implementado");
    }

    @Override
    public void deleteByUserName(String username) {
        String sql = "DELETE FROM `User` WHERE username = ?";
        try (Connection con = MyDataSource.getDataSource().getConnection();
             PreparedStatement ps = con.prepareStatement(sql)) {
            ps.setString(1, username);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RuntimeException("deleteByUserName", e);
        }
    }
}