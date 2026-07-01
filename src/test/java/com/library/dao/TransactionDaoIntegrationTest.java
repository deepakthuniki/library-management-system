package com.library.dao;

import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.model.User;
import com.library.util.DatabaseConnection;
import com.library.util.PasswordUtil;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import java.math.BigDecimal;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.Year;

import static org.junit.Assert.*;

/**
 * Integration test: runs against a real MySQL instance (see db.properties /
 * DB_URL env var) to verify that issueBook/returnBook are truly atomic -
 * the transactions row and the books.available_copies update either both
 * commit or both roll back together.
 *
 * Requires a running MySQL with schema.sql applied. Run via:
 *   mvn -Dtest=TransactionDaoIntegrationTest test
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class TransactionDaoIntegrationTest {

    private static BookDao bookDao;
    private static MemberDao memberDao;
    private static UserDao userDao;
    private static TransactionDao transactionDao;

    private static int testBookId;
    private static int testMemberId;
    private static int testUserId;

    @BeforeClass
    public static void setUpClass() throws SQLException {
        bookDao = new BookDao();
        memberDao = new MemberDao();
        userDao = new UserDao();
        transactionDao = new TransactionDao();

        // Isolated test fixtures - unique ISBN/email per run avoids collisions
        String uniqueSuffix = String.format("%010d", System.nanoTime() % 10_000_000_000L);

        Book book = new Book("999" + uniqueSuffix, "Integration Test Book",
                "Test Author", "Testing", "Test Publisher", Year.of(2024), 1);
        book = bookDao.save(book);
        testBookId = book.getBookId();

        Member member = new Member("Integration Test Member",
                "itest" + uniqueSuffix + "@example.com", "9123456789", Member.MemberType.GENERAL);
        member = memberDao.save(member);
        testMemberId = member.getMemberId();

        User user = new User("itest_user_" + uniqueSuffix, PasswordUtil.hash("pw"),
                "Integration Test User", User.Role.LIBRARIAN);
        user = userDao.save(user);
        testUserId = user.getUserId();
    }

    @AfterClass
    public static void tearDownClass() {
        DatabaseConnection.shutdown();
    }

    @Test
    public void test1_issueBook_atomicallyCommitsTransactionAndDecrementsCopies() throws SQLException {
        Book before = bookDao.findById(testBookId).orElseThrow();
        assertEquals(1, before.getAvailableCopies());

        Transaction txn = new Transaction(testBookId, testMemberId, testUserId,
                LocalDate.now(), LocalDate.now().plusDays(14));
        Transaction saved = transactionDao.issueBook(txn);

        assertTrue("Transaction ID should be generated", saved.getTxnId() > 0);

        Book after = bookDao.findById(testBookId).orElseThrow();
        assertEquals("Available copies must decrement atomically with the issue",
                0, after.getAvailableCopies());
    }

    @Test
    public void test2_issueBook_rollsBackWhenNoCopiesAvailable() {
        // At this point (after the previous test) available_copies is 0
        Transaction txn = new Transaction(testBookId, testMemberId, testUserId,
                LocalDate.now(), LocalDate.now().plusDays(14));

        assertThrows(SQLException.class, () -> transactionDao.issueBook(txn));

        // Verify no phantom transaction row was left behind by the failed attempt
        assertDoesNotThrow(() -> {
            long countBefore = transactionDao.findAll().stream()
                    .filter(t -> t.getBookId() == testBookId)
                    .count();
            assertEquals("Only the one successful issue should exist, no rollback leftovers",
                    1, countBefore);
        });
    }

    @Test
    public void test3_returnBook_atomicallyUpdatesStatusAndIncrementsCopies() throws SQLException {
        // Find the active transaction created in the first test
        Transaction active = transactionDao.findActiveByMember(testMemberId).stream()
                .filter(t -> t.getBookId() == testBookId)
                .findFirst()
                .orElseThrow(() -> new AssertionError("Expected an active loan for setup"));

        boolean result = transactionDao.returnBook(active.getTxnId(), LocalDate.now(), BigDecimal.ZERO);
        assertTrue(result);

        Book afterReturn = bookDao.findById(testBookId).orElseThrow();
        assertEquals("Available copies must increment atomically with the return",
                1, afterReturn.getAvailableCopies());

        Transaction updated = transactionDao.findById(active.getTxnId()).orElseThrow();
        assertEquals(Transaction.Status.RETURNED, updated.getStatus());
    }

    private static void assertDoesNotThrow(ThrowingRunnable r) {
        try {
            r.run();
        } catch (Exception e) {
            fail("Expected no exception but got: " + e);
        }
    }

    private interface ThrowingRunnable {
        void run() throws Exception;
    }
}
