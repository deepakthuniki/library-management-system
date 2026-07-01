package com.library.model;

import java.time.LocalDate;
import java.math.BigDecimal;

/**
 * Represents a single issue/return transaction linking a Book and a Member.
 */
public class Transaction {

    public enum Status { ISSUED, RETURNED, OVERDUE, LOST }

    private long txnId;
    private int bookId;
    private int memberId;
    private int issuedBy;
    private LocalDate issueDate;
    private LocalDate dueDate;
    private LocalDate returnDate;
    private BigDecimal fineAmount;
    private Status status;

    // Denormalized display fields (populated by joins for reporting; not persisted directly)
    private String bookTitle;
    private String memberName;

    public Transaction(int bookId, int memberId, int issuedBy, LocalDate issueDate, LocalDate dueDate) {
        this.bookId = bookId;
        this.memberId = memberId;
        this.issuedBy = issuedBy;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.fineAmount = BigDecimal.ZERO;
        this.status = Status.ISSUED;
    }

    public Transaction(long txnId, int bookId, int memberId, int issuedBy, LocalDate issueDate,
                        LocalDate dueDate, LocalDate returnDate, BigDecimal fineAmount, Status status) {
        this.txnId = txnId;
        this.bookId = bookId;
        this.memberId = memberId;
        this.issuedBy = issuedBy;
        this.issueDate = issueDate;
        this.dueDate = dueDate;
        this.returnDate = returnDate;
        this.fineAmount = fineAmount;
        this.status = status;
    }

    public boolean isOverdue(LocalDate asOf) {
        return status == Status.ISSUED && asOf.isAfter(dueDate);
    }

    public long getTxnId() { return txnId; }
    public void setTxnId(long txnId) { this.txnId = txnId; }

    public int getBookId() { return bookId; }
    public int getMemberId() { return memberId; }
    public int getIssuedBy() { return issuedBy; }

    public LocalDate getIssueDate() { return issueDate; }
    public LocalDate getDueDate() { return dueDate; }

    public LocalDate getReturnDate() { return returnDate; }
    public void setReturnDate(LocalDate returnDate) { this.returnDate = returnDate; }

    public BigDecimal getFineAmount() { return fineAmount; }
    public void setFineAmount(BigDecimal fineAmount) { this.fineAmount = fineAmount; }

    public Status getStatus() { return status; }
    public void setStatus(Status status) { this.status = status; }

    public String getBookTitle() { return bookTitle; }
    public void setBookTitle(String bookTitle) { this.bookTitle = bookTitle; }

    public String getMemberName() { return memberName; }
    public void setMemberName(String memberName) { this.memberName = memberName; }
}
