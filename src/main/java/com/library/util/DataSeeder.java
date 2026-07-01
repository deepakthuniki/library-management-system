package com.library.util;

import com.library.dao.BookDao;
import com.library.dao.MemberDao;
import com.library.dao.UserDao;
import com.library.model.Book;
import com.library.model.Member;
import com.library.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.SQLException;
import java.time.Year;
import java.util.Random;

/**
 * Seeds the database with a default admin account and a bulk of demo
 * books/members so the app can be demonstrated at "500+ records" scale,
 * matching the resume claim. Only runs if tables are empty.
 */
public final class DataSeeder {

    private static final Logger log = LoggerFactory.getLogger(DataSeeder.class);
    private static final Random random = new Random(42);

    private static final String[] CATEGORIES = {
            "Fiction", "Science", "Technology", "History", "Biography",
            "Mathematics", "Philosophy", "Fantasy", "Mystery", "Self-Help"
    };
    private static final String[] AUTHORS = {
            "R. K. Sharma", "A. Verma", "J. Doe", "S. Patel", "M. Rao",
            "K. Iyer", "L. Fernandez", "T. Nakamura", "E. Johnson", "P. Gupta"
    };
    private static final String[] FIRST_NAMES = {
            "Aarav", "Vivaan", "Aditya", "Diya", "Ananya", "Ishaan", "Kabir",
            "Meera", "Riya", "Sai", "Neha", "Arjun", "Priya", "Rohan", "Sneha"
    };
    private static final String[] LAST_NAMES = {
            "Reddy", "Rao", "Sharma", "Verma", "Kumar", "Nair", "Gupta",
            "Mehta", "Das", "Chowdhury", "Iyer", "Menon"
    };

    private DataSeeder() { }

    public static void seedIfEmpty() throws SQLException {
        UserDao userDao = new UserDao();
        BookDao bookDao = new BookDao();
        MemberDao memberDao = new MemberDao();

        if (userDao.findByUsername("admin").isEmpty()) {
            User admin = new User("admin", PasswordUtil.hash("admin123"), "System Administrator", User.Role.ADMIN);
            userDao.save(admin);
            User librarian = new User("librarian", PasswordUtil.hash("lib123"), "Default Librarian", User.Role.LIBRARIAN);
            userDao.save(librarian);
            log.info("Seeded default users: admin/admin123, librarian/lib123");
        }

        if (bookDao.count() == 0) {
            log.info("Seeding 500 demo books...");
            for (int i = 1; i <= 500; i++) {
                String isbn = String.format("978%010d", i);
                String title = randomCategory() + " Insights Vol. " + i;
                Book book = new Book(isbn, title, randomAuthor(), randomCategory(),
                        "Demo Publishing House", Year.of(2000 + random.nextInt(25)),
                        1 + random.nextInt(5));
                bookDao.save(book);
            }
            log.info("Seeded 500 books.");
        }

        if (memberDao.count() == 0) {
            log.info("Seeding 120 demo members...");
            for (int i = 1; i <= 120; i++) {
                String first = FIRST_NAMES[random.nextInt(FIRST_NAMES.length)];
                String last = LAST_NAMES[random.nextInt(LAST_NAMES.length)];
                String name = first + " " + last;
                String email = (first + "." + last + i + "@example.com").toLowerCase();
                String phone = "9" + String.format("%09d", random.nextInt(1_000_000_000));
                Member.MemberType type = Member.MemberType.values()[random.nextInt(3)];
                Member m = new Member(name, email, phone, type);
                memberDao.save(m);
            }
            log.info("Seeded 120 members.");
        }
    }

    private static String randomCategory() {
        return CATEGORIES[random.nextInt(CATEGORIES.length)];
    }

    private static String randomAuthor() {
        return AUTHORS[random.nextInt(AUTHORS.length)];
    }
}
