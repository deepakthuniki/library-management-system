package com.library.service;

import com.library.dao.BookDao;
import com.library.dao.MemberDao;
import com.library.dao.TransactionDao;
import com.library.exception.BookNotAvailableException;
import com.library.exception.MemberLimitExceededException;
import com.library.exception.RecordNotFoundException;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.Transaction;
import com.library.util.DatabaseConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public class TransactionService {

    private static final Logger log = LoggerFactory.getLogger(TransactionService.class);
    private static final int LOAN_PERIOD_DAYS = 14;
    private static final BigDecimal FINE_PER_DAY = new BigDecimal("5.00"); // currency units/day

    private final TransactionDao transactionDao;
    private final BookDao bookDao;
    private final MemberDao memberDao;

    public TransactionService(TransactionDao transactionDao, BookDao bookDao, MemberDao memberDao) {
        this.transactionDao = transactionDao;
        this.bookDao = bookDao;
        this.memberDao = memberDao;
    }

    /**
     * Issues a book after validating business rules:
     *  - Book must exist and have available copies
     *  - Member must be ACTIVE and under their loan limit
     * The actual DB writes are atomic (see TransactionDao.issueBook).
     */
    public Transaction issueBook(int bookId, int memberId, int issuedByUserId)
            throws SQLException, RecordNotFoundException, BookNotAvailableException, MemberLimitExceededException {

        Book book = bookDao.findById(bookId)
                .orElseThrow(() -> new RecordNotFoundException("Book not found: " + bookId));
        if (!book.isAvailable()) {
            throw new BookNotAvailableException("No copies available for: " + book.getTitle());
        }

        Member member = memberDao.findById(memberId)
                .orElseThrow(() -> new RecordNotFoundException("Member not found: " + memberId));
        if (!member.isActive()) {
            throw new MemberLimitExceededException("Member is not active: " + member.getFullName());
        }

        // Check current loan count against member's limit (read-then-write race is
        // acceptable here since this is a single-process console app; a multi-user
        // web version would need this check inside the same DB transaction with locking)
        try (Connection conn = DatabaseConnection.getConnection()) {
            int activeLoans = memberDao.countActiveLoans(conn, memberId);
            if (activeLoans >= member.getMaxBooksAllowed()) {
                throw new MemberLimitExceededException(
                        member.getFullName() + " has reached their loan limit (" + member.getMaxBooksAllowed() + ")");
            }
        }

        LocalDate issueDate = LocalDate.now();
        LocalDate dueDate = issueDate.plusDays(LOAN_PERIOD_DAYS);
        Transaction txn = new Transaction(bookId, memberId, issuedByUserId, issueDate, dueDate);
        return transactionDao.issueBook(txn);
    }

    /**
     * Returns a book, computing any overdue fine, atomically.
     */
    public Transaction returnBook(long txnId) throws SQLException, RecordNotFoundException {
        Transaction txn = transactionDao.findById(txnId)
                .orElseThrow(() -> new RecordNotFoundException("Transaction not found: " + txnId));

        LocalDate returnDate = LocalDate.now();
        BigDecimal fine = calculateFine(txn.getDueDate(), returnDate);

        boolean ok = transactionDao.returnBook(txnId, returnDate, fine);
        if (!ok) {
            throw new RecordNotFoundException("Transaction already returned or not found: " + txnId);
        }
        txn.setReturnDate(returnDate);
        txn.setFineAmount(fine);
        txn.setStatus(Transaction.Status.RETURNED);
        log.info("Book returned for txn {} with fine {}", txnId, fine);
        return txn;
    }

    private BigDecimal calculateFine(LocalDate dueDate, LocalDate returnDate) {
        long overdueDays = ChronoUnit.DAYS.between(dueDate, returnDate);
        if (overdueDays <= 0) return BigDecimal.ZERO;
        return FINE_PER_DAY.multiply(BigDecimal.valueOf(overdueDays));
    }

    public List<Transaction> activeLoansForMember(int memberId) throws SQLException {
        return transactionDao.findActiveByMember(memberId);
    }

    public List<Transaction> overdueTransactions() throws SQLException {
        return transactionDao.findOverdue(LocalDate.now());
    }

    public List<Transaction> allTransactions() throws SQLException {
        return transactionDao.findAll();
    }
}
