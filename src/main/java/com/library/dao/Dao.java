package com.library.dao;

import java.sql.SQLException;
import java.util.List;
import java.util.Optional;

/**
 * Generic CRUD contract. Reused by BookDao/MemberDao/UserDao to avoid
 * duplicating boilerplate method signatures (reduces duplication).
 */
public interface Dao<T, ID> {
    T save(T entity) throws SQLException;
    Optional<T> findById(ID id) throws SQLException;
    List<T> findAll() throws SQLException;
    boolean update(T entity) throws SQLException;
    boolean deleteById(ID id) throws SQLException;
}
