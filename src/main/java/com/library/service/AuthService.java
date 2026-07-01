package com.library.service;

import com.library.dao.UserDao;
import com.library.exception.AuthenticationException;
import com.library.model.User;
import com.library.util.PasswordUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;

public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);
    private final UserDao userDao;

    public AuthService(UserDao userDao) {
        this.userDao = userDao;
    }

    public User login(String username, String password) throws SQLException, AuthenticationException {
        User user = userDao.findByUsername(username)
                .orElseThrow(() -> new AuthenticationException("Invalid username or password"));
        if (!PasswordUtil.verify(password, user.getPasswordHash())) {
            log.warn("Failed login attempt for username: {}", username);
            throw new AuthenticationException("Invalid username or password");
        }
        log.info("User logged in: {} ({})", username, user.getRole());
        return user;
    }

    public User registerStaff(String username, String password, String fullName, User.Role role)
            throws SQLException, AuthenticationException {
        if (userDao.findByUsername(username).isPresent()) {
            throw new AuthenticationException("Username already taken: " + username);
        }
        User user = new User(username, PasswordUtil.hash(password), fullName, role);
        return userDao.save(user);
    }
}
