package com.library.model;

import org.junit.Test;
import java.time.Year;
import static org.junit.Assert.*;

public class BookTest {

    private Book newBook(int copies) {
        return new Book("9780306406157", "Test Book", "Test Author",
                "Fiction", "Test Publisher", Year.of(2020), copies);
    }

    @Test
    public void newBook_allCopiesAvailableInitially() {
        Book b = newBook(3);
        assertEquals(3, b.getAvailableCopies());
        assertTrue(b.isAvailable());
    }

    @Test
    public void borrowCopy_decrementsAvailableCopies() {
        Book b = newBook(2);
        b.borrowCopy();
        assertEquals(1, b.getAvailableCopies());
    }

    @Test(expected = IllegalStateException.class)
    public void borrowCopy_throwsWhenNoneAvailable() {
        Book b = newBook(1);
        b.borrowCopy();
        b.borrowCopy(); // should throw - no copies left
    }

    @Test
    public void returnCopy_incrementsAvailableCopies() {
        Book b = newBook(2);
        b.borrowCopy();
        b.returnCopy();
        assertEquals(2, b.getAvailableCopies());
    }

    @Test(expected = IllegalStateException.class)
    public void returnCopy_throwsWhenAtFullCapacity() {
        Book b = newBook(2);
        b.returnCopy(); // already full - should throw
    }

    @Test
    public void isAvailable_falseWhenZeroCopiesLeft() {
        Book b = newBook(1);
        b.borrowCopy();
        assertFalse(b.isAvailable());
    }
}
