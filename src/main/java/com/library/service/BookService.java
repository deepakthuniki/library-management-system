package com.library.service;

import com.library.dao.BookDao;
import com.library.exception.DuplicateRecordException;
import com.library.exception.RecordNotFoundException;
import com.library.model.Book;
import com.library.util.ValidationUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;

public class BookService {

    private static final Logger log = LoggerFactory.getLogger(BookService.class);
    private final BookDao bookDao;

    public BookService(BookDao bookDao) {
        this.bookDao = bookDao;
    }

    public Book addBook(String isbn, String title, String author, String category,
                         String publisher, Year year, int copies) throws SQLException, DuplicateRecordException {
        if (!ValidationUtil.isValidIsbn(isbn)) {
            throw new IllegalArgumentException("Invalid ISBN format: " + isbn);
        }
        if (!ValidationUtil.isNonBlank(title) || !ValidationUtil.isNonBlank(author)) {
            throw new IllegalArgumentException("Title and author are required");
        }
        if (!ValidationUtil.isPositive(copies)) {
            throw new IllegalArgumentException("Copies must be a positive number");
        }
        if (bookDao.findByIsbn(isbn).isPresent()) {
            throw new DuplicateRecordException("A book with ISBN " + isbn + " already exists");
        }
        Book book = new Book(isbn, title, author, category, publisher, year, copies);
        return bookDao.save(book);
    }

    public Book getBook(int bookId) throws SQLException, RecordNotFoundException {
        return bookDao.findById(bookId)
                .orElseThrow(() -> new RecordNotFoundException("No book found with ID " + bookId));
    }

    public List<Book> listAll() throws SQLException {
        return bookDao.findAll();
    }

    public List<Book> search(String title, String author, String category, Boolean onlyAvailable) throws SQLException {
        return bookDao.search(title, author, category, onlyAvailable);
    }

    public void updateBook(Book book) throws SQLException, RecordNotFoundException {
        boolean updated = bookDao.update(book);
        if (!updated) {
            throw new RecordNotFoundException("Cannot update - book not found: " + book.getBookId());
        }
        log.info("Book updated: {}", book.getBookId());
    }

    public void deleteBook(int bookId) throws SQLException, RecordNotFoundException {
        boolean deleted = bookDao.deleteById(bookId);
        if (!deleted) {
            throw new RecordNotFoundException("Cannot delete - book not found: " + bookId);
        }
        log.info("Book deleted: {}", bookId);
    }

    public int totalBookCount() throws SQLException {
        return bookDao.count();
    }
}
