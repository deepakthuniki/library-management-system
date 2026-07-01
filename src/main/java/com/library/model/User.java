package com.library.model;

/**
 * A staff account (Admin or Librarian) that can log in and operate the system.
 * Extends Person (inheritance) - demonstrates polymorphism via describeRole().
 */
public class User extends Person {

    public enum Role { ADMIN, LIBRARIAN }

    private int userId;
    private String username;
    private String passwordHash; // BCrypt hash, never plaintext
    private Role role;

    public User(String username, String passwordHash, String fullName, Role role) {
        super(fullName, username);
        this.username = username;
        this.passwordHash = passwordHash;
        this.role = role;
    }

    public User(int userId, String username, String passwordHash, String fullName, Role role) {
        this(username, passwordHash, fullName, role);
        this.userId = userId;
    }

    @Override
    public String describeRole() {
        return role.name();
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    public int getUserId() { return userId; }
    public void setUserId(int userId) { this.userId = userId; }

    public String getUsername() { return username; }

    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }

    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
}
