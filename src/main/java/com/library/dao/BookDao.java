package com.library.dao;

import com.library.model.Book;
import com.library.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.*;
import java.time.Year;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Data access for the books table. All queries use PreparedStatement
 * (prevents SQL injection) and rely on the indexed columns (title, author,
 * category, isbn) defined in schema.sql for efficient filtering.
 */
public class BookDao implements Dao<Book, Integer> {

    private static final Logger log = LoggerFactory.getLogger(BookDao.class);

    @Override
    public Book save(Book book) throws SQLException {
        String sql = """
            INSERT INTO books (isbn, title, author, category, publisher, publish_year, total_copies, available_copies)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindBook(ps, book);
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    book.setBookId(keys.getInt(1));
                }
            }
            log.info("Book saved: {}", book.getTitle());
            return book;
        }
    }

    private void bindBook(PreparedStatement ps, Book b) throws SQLException {
        ps.setString(1, b.getIsbn());
        ps.setString(2, b.getTitle());
        ps.setString(3, b.getAuthor());
        ps.setString(4, b.getCategory());
        ps.setString(5, b.getPublisher());
        if (b.getPublishYear() != null) {
            ps.setInt(6, b.getPublishYear().getValue());
        } else {
            ps.setNull(6, Types.INTEGER);
        }
        ps.setInt(7, b.getTotalCopies());
        ps.setInt(8, b.getAvailableCopies());
    }

    @Override
    public Optional<Book> findById(Integer id) throws SQLException {
        String sql = "SELECT * FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    public Optional<Book> findByIsbn(String isbn) throws SQLException {
        String sql = "SELECT * FROM books WHERE isbn = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, isbn);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    @Override
    public List<Book> findAll() throws SQLException {
        String sql = "SELECT * FROM books ORDER BY title";
        List<Book> books = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                books.add(mapRow(rs));
            }
        }
        return books;
    }

    /**
     * Complex filtering query - demonstrates optimized SQL using indexed
     * columns (title/author/category) with dynamic WHERE composition.
     */
    public List<Book> search(String title, String author, String category, Boolean onlyAvailable)
            throws SQLException {
        StringBuilder sql = new StringBuilder("SELECT * FROM books WHERE 1=1");
        List<Object> params = new ArrayList<>();

        if (title != null && !title.isBlank()) {
            sql.append(" AND title LIKE ?");
            params.add("%" + title + "%");
        }
        if (author != null && !author.isBlank()) {
            sql.append(" AND author LIKE ?");
            params.add("%" + author + "%");
        }
        if (category != null && !category.isBlank()) {
            sql.append(" AND category = ?");
            params.add(category);
        }
        if (Boolean.TRUE.equals(onlyAvailable)) {
            sql.append(" AND available_copies > 0");
        }
        sql.append(" ORDER BY title");

        List<Book> results = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql.toString())) {
            for (int i = 0; i < params.size(); i++) {
                ps.setObject(i + 1, params.get(i));
            }
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    results.add(mapRow(rs));
                }
            }
        }
        return results;
    }

    @Override
    public boolean update(Book book) throws SQLException {
        String sql = """
            UPDATE books SET isbn=?, title=?, author=?, category=?, publisher=?,
                publish_year=?, total_copies=?, available_copies=? WHERE book_id=?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            bindBook(ps, book);
            ps.setInt(9, book.getBookId());
            return ps.executeUpdate() > 0;
        }
    }

    /**
     * Atomically decrement available_copies. Used inside a larger transaction
     * (see TransactionDao.issueBook) so the caller controls commit/rollback.
     */
    public boolean decrementAvailableCopies(Connection conn, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies - 1 " +
                     "WHERE book_id = ? AND available_copies > 0";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    public boolean incrementAvailableCopies(Connection conn, int bookId) throws SQLException {
        String sql = "UPDATE books SET available_copies = available_copies + 1 " +
                     "WHERE book_id = ? AND available_copies < total_copies";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, bookId);
            return ps.executeUpdate() > 0;
        }
    }

    @Override
    public boolean deleteById(Integer id) throws SQLException {
        String sql = "DELETE FROM books WHERE book_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            return ps.executeUpdate() > 0;
        }
    }

    public int count() throws SQLException {
        String sql = "SELECT COUNT(*) FROM books";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Book mapRow(ResultSet rs) throws SQLException {
        int year = rs.getInt("publish_year");
        return new Book(
                rs.getInt("book_id"),
                rs.getString("isbn"),
                rs.getString("title"),
                rs.getString("author"),
                rs.getString("category"),
                rs.getString("publisher"),
                rs.wasNull() ? null : Year.of(year),
                rs.getInt("total_copies"),
                rs.getInt("available_copies")
        );
    }
}
