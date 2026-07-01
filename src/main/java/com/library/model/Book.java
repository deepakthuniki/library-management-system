package com.library.model;

import java.time.Year;

/**
 * A catalog entry. Encapsulation: copy counts are only mutated via
 * controlled methods (borrowCopy/returnCopy) so invariants always hold.
 */
public class Book {

    private int bookId;
    private String isbn;
    private String title;
    private String author;
    private String category;
    private String publisher;
    private Year publishYear;
    private int totalCopies;
    private int availableCopies;

    public Book(String isbn, String title, String author, String category,
                String publisher, Year publishYear, int totalCopies) {
        this.isbn = isbn;
        this.title = title;
        this.author = author;
        this.category = category;
        this.publisher = publisher;
        this.publishYear = publishYear;
        this.totalCopies = totalCopies;
        this.availableCopies = totalCopies;
    }

    public Book(int bookId, String isbn, String title, String author, String category,
                String publisher, Year publishYear, int totalCopies, int availableCopies) {
        this(isbn, title, author, category, publisher, publishYear, totalCopies);
        this.bookId = bookId;
        this.availableCopies = availableCopies;
    }

    public boolean isAvailable() {
        return availableCopies > 0;
    }

    /** Decrement available copies when a copy is issued. Enforces invariant. */
    public void borrowCopy() {
        if (availableCopies <= 0) {
            throw new IllegalStateException("No available copies of: " + title);
        }
        availableCopies--;
    }

    /** Increment available copies when a copy is returned. Enforces invariant. */
    public void returnCopy() {
        if (availableCopies >= totalCopies) {
            throw new IllegalStateException("All copies already accounted for: " + title);
        }
        availableCopies++;
    }

    public int getBookId() { return bookId; }
    public void setBookId(int bookId) { this.bookId = bookId; }

    public String getIsbn() { return isbn; }
    public void setIsbn(String isbn) { this.isbn = isbn; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getPublisher() { return publisher; }
    public void setPublisher(String publisher) { this.publisher = publisher; }

    public Year getPublishYear() { return publishYear; }
    public void setPublishYear(Year publishYear) { this.publishYear = publishYear; }

    public int getTotalCopies() { return totalCopies; }
    public void setTotalCopies(int totalCopies) { this.totalCopies = totalCopies; }

    public int getAvailableCopies() { return availableCopies; }
    public void setAvailableCopies(int availableCopies) { this.availableCopies = availableCopies; }

    @Override
    public String toString() {
        return String.format("[%d] %s by %s (%s) - %d/%d available",
                bookId, title, author, category, availableCopies, totalCopies);
    }
}
