package com.library.service;

import com.library.dao.BookDao;
import com.library.exception.DuplicateRecordException;
import com.library.model.Book;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.time.Year;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for BookService using Mockito to mock the DAO layer -
 * verifies business rules (validation, duplicate detection) in isolation
 * from the database.
 */
@RunWith(MockitoJUnitRunner.class)
public class BookServiceTest {

    @Mock
    private BookDao bookDao;

    private BookService bookService;

    @Before
    public void setUp() {
        bookService = new BookService(bookDao);
    }

    @Test
    public void addBook_rejectsInvalidIsbn() {
        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook("bad-isbn", "Title", "Author", "Fiction",
                        "Publisher", Year.of(2020), 1));
    }

    @Test
    public void addBook_rejectsBlankTitle() {
        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook("9780306406157", "  ", "Author", "Fiction",
                        "Publisher", Year.of(2020), 1));
    }

    @Test
    public void addBook_rejectsZeroCopies() {
        assertThrows(IllegalArgumentException.class, () ->
                bookService.addBook("9780306406157", "Title", "Author", "Fiction",
                        "Publisher", Year.of(2020), 0));
    }

    @Test
    public void addBook_rejectsDuplicateIsbn() throws Exception {
        when(bookDao.findByIsbn("9780306406157"))
                .thenReturn(Optional.of(mock(Book.class)));

        assertThrows(DuplicateRecordException.class, () ->
                bookService.addBook("9780306406157", "Title", "Author", "Fiction",
                        "Publisher", Year.of(2020), 2));

        verify(bookDao, never()).save(any());
    }

    @Test
    public void addBook_savesWhenValidAndUnique() throws Exception {
        when(bookDao.findByIsbn("9780306406157")).thenReturn(Optional.empty());
        when(bookDao.save(any(Book.class))).thenAnswer(inv -> inv.getArgument(0));

        Book saved = bookService.addBook("9780306406157", "Clean Code", "R. Martin",
                "Technology", "Prentice Hall", Year.of(2008), 3);

        assertEquals("Clean Code", saved.getTitle());
        verify(bookDao, times(1)).save(any(Book.class));
    }
}
