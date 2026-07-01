package com.library.dao;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.library.model.Transaction;
import com.library.util.DatabaseConnection;

import java.math.BigDecimal;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Handles issue/return transactions. This is where ACID compliance is
 * enforced explicitly: issueBook() and returnBook() each wrap multiple
 * writes (transactions table + books table) in a single JDBC transaction,
 * using manual commit()/rollback() so a partial failure never leaves
 * available_copies out of sync with actual issued books.
 */
public class TransactionDao {

    private static final Logger log = LoggerFactory.getLogger(TransactionDao.class);
    private final BookDao bookDao = new BookDao();

    /**
     * Issues a book to a member atomically:
     *  1. Insert transaction row
     *  2. Decrement book's available_copies
     * Both succeed together or neither is applied (rollback on any failure).
     */
    public Transaction issueBook(Transaction txn) throws SQLException {
        String insertSql = """
            INSERT INTO transactions (book_id, member_id, issued_by, issue_date, due_date, status)
            VALUES (?, ?, ?, ?, ?, 'ISSUED')
            """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false); // begin transaction

            try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                ps.setInt(1, txn.getBookId());
                ps.setInt(2, txn.getMemberId());
                ps.setInt(3, txn.getIssuedBy());
                ps.setDate(4, Date.valueOf(txn.getIssueDate()));
                ps.setDate(5, Date.valueOf(txn.getDueDate()));
                ps.executeUpdate();
                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (keys.next()) txn.setTxnId(keys.getLong(1));
                }
            }

            boolean decremented = bookDao.decrementAvailableCopies(conn, txn.getBookId());
            if (!decremented) {
                // No copies available -> abort the whole transaction
                conn.rollback();
                log.warn("Rollback: no available copies for book_id={}", txn.getBookId());
                throw new SQLException("No available copies for book_id=" + txn.getBookId());
            }

            conn.commit(); // both writes succeeded -> persist atomically
            log.info("Book issued: txn={}, book={}, member={}", txn.getTxnId(), txn.getBookId(), txn.getMemberId());
            return txn;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.error("Transaction rolled back for issueBook", e);
                } catch (SQLException rollbackEx) {
                    log.error("Rollback failed", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    /**
     * Returns a book atomically:
     *  1. Update transaction row (return_date, status, fine)
     *  2. Increment book's available_copies
     */
    public boolean returnBook(long txnId, LocalDate returnDate, BigDecimal fine) throws SQLException {
        String selectSql = "SELECT book_id FROM transactions WHERE txn_id = ? AND status = 'ISSUED' FOR UPDATE";
        String updateSql = """
            UPDATE transactions SET return_date=?, fine_amount=?, status='RETURNED' WHERE txn_id=?
            """;

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            int bookId;
            try (PreparedStatement ps = conn.prepareStatement(selectSql)) {
                ps.setLong(1, txnId);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        log.warn("Rollback: txn {} not found or already returned", txnId);
                        return false;
                    }
                    bookId = rs.getInt("book_id");
                }
            }

            try (PreparedStatement ps = conn.prepareStatement(updateSql)) {
                ps.setDate(1, Date.valueOf(returnDate));
                ps.setBigDecimal(2, fine);
                ps.setLong(3, txnId);
                ps.executeUpdate();
            }

            bookDao.incrementAvailableCopies(conn, bookId);

            conn.commit();
            log.info("Book returned: txn={}, fine={}", txnId, fine);
            return true;

        } catch (SQLException e) {
            if (conn != null) {
                try {
                    conn.rollback();
                    log.error("Transaction rolled back for returnBook", e);
                } catch (SQLException rollbackEx) {
                    log.error("Rollback failed", rollbackEx);
                }
            }
            throw e;
        } finally {
            if (conn != null) {
                conn.setAutoCommit(true);
                conn.close();
            }
        }
    }

    public List<Transaction> findActiveByMember(int memberId) throws SQLException {
        String sql = """
            SELECT t.*, b.title AS book_title, m.full_name AS member_name
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.member_id
            WHERE t.member_id = ? AND t.status = 'ISSUED'
            ORDER BY t.due_date
            """;
        return queryList(sql, memberId);
    }

    public List<Transaction> findOverdue(LocalDate asOf) throws SQLException {
        String sql = """
            SELECT t.*, b.title AS book_title, m.full_name AS member_name
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.member_id
            WHERE t.status = 'ISSUED' AND t.due_date < ?
            ORDER BY t.due_date
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(asOf));
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        }
    }

    public List<Transaction> findAll() throws SQLException {
        String sql = """
            SELECT t.*, b.title AS book_title, m.full_name AS member_name
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.member_id
            ORDER BY t.issue_date DESC
            """;
        List<Transaction> result = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) result.add(mapRow(rs));
        }
        return result;
    }

    public Optional<Transaction> findById(long txnId) throws SQLException {
        String sql = """
            SELECT t.*, b.title AS book_title, m.full_name AS member_name
            FROM transactions t
            JOIN books b ON t.book_id = b.book_id
            JOIN members m ON t.member_id = m.member_id
            WHERE t.txn_id = ?
            """;
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setLong(1, txnId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        }
    }

    private List<Transaction> queryList(String sql, int param) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, param);
            try (ResultSet rs = ps.executeQuery()) {
                List<Transaction> result = new ArrayList<>();
                while (rs.next()) result.add(mapRow(rs));
                return result;
            }
        }
    }

    private Transaction mapRow(ResultSet rs) throws SQLException {
        Date ret = rs.getDate("return_date");
        Transaction t = new Transaction(
                rs.getLong("txn_id"),
                rs.getInt("book_id"),
                rs.getInt("member_id"),
                rs.getInt("issued_by"),
                rs.getDate("issue_date").toLocalDate(),
                rs.getDate("due_date").toLocalDate(),
                ret != null ? ret.toLocalDate() : null,
                rs.getBigDecimal("fine_amount"),
                Transaction.Status.valueOf(rs.getString("status"))
        );
        t.setBookTitle(rs.getString("book_title"));
        t.setMemberName(rs.getString("member_name"));
        return t;
    }
}
