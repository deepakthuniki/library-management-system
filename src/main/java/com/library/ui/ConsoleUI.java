package com.library.ui;

import com.library.dao.*;
import com.library.exception.*;
import com.library.model.*;
import com.library.service.*;

import java.sql.SQLException;
import java.time.Year;
import java.util.List;
import java.util.Scanner;

/**
 * Menu-driven console interface. Kept separate from business logic (service
 * layer) so the UI could later be swapped (e.g. web) without touching rules.
 */
public class ConsoleUI {

    private final Scanner sc = new Scanner(System.in);
    private final AuthService authService = new AuthService(new UserDao());
    private final BookService bookService = new BookService(new BookDao());
    private final MemberService memberService = new MemberService(new MemberDao());
    private final TransactionService txnService =
            new TransactionService(new TransactionDao(), new BookDao(), new MemberDao());

    private User currentUser;

    public void start() {
        System.out.println("=========================================");
        System.out.println("   LIBRARY MANAGEMENT SYSTEM (Java/JDBC)");
        System.out.println("=========================================");
        if (!login()) {
            System.out.println("Too many failed attempts. Exiting.");
            return;
        }
        mainMenu();
    }

    private boolean login() {
        for (int attempt = 0; attempt < 3; attempt++) {
            System.out.print("Username: ");
            String username = sc.nextLine().trim();
            System.out.print("Password: ");
            String password = sc.nextLine().trim();
            try {
                currentUser = authService.login(username, password);
                System.out.println("Welcome, " + currentUser.getFullName() + " [" + currentUser.getRole() + "]");
                return true;
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (AuthenticationException e) {
                System.out.println(e.getMessage() + " (" + (2 - attempt) + " attempts left)");
            }
        }
        return false;
    }

    private void mainMenu() {
        boolean running = true;
        while (running) {
            System.out.println("\n----- MAIN MENU -----");
            System.out.println("1. Book Catalog");
            System.out.println("2. Member Management");
            System.out.println("3. Issue / Return Books");
            System.out.println("4. Reports");
            System.out.println("0. Logout & Exit");
            System.out.print("Choose: ");
            String choice = sc.nextLine().trim();
            try {
                switch (choice) {
                    case "1" -> bookMenu();
                    case "2" -> memberMenu();
                    case "3" -> transactionMenu();
                    case "4" -> reportsMenu();
                    case "0" -> running = false;
                    default -> System.out.println("Invalid choice.");
                }
            } catch (SQLException e) {
                System.out.println("Database error: " + e.getMessage());
            } catch (LibraryException e) {
                System.out.println("Error: " + e.getMessage());
            } catch (IllegalArgumentException e) {
                System.out.println("Invalid input: " + e.getMessage());
            }
        }
        System.out.println("Goodbye!");
    }

    // ---------------- BOOK MENU ----------------
    private void bookMenu() throws SQLException, LibraryException {
        System.out.println("\n-- Book Catalog --");
        System.out.println("1. Add Book  2. Search  3. List All  4. Update  5. Delete  0. Back");
        System.out.print("Choose: ");
        switch (sc.nextLine().trim()) {
            case "1" -> addBook();
            case "2" -> searchBooks();
            case "3" -> printBooks(bookService.listAll());
            case "4" -> updateBook();
            case "5" -> deleteBook();
            case "0" -> { }
            default -> System.out.println("Invalid choice.");
        }
    }

    private void addBook() throws SQLException, DuplicateRecordException {
        System.out.print("ISBN (10/13 digit): "); String isbn = sc.nextLine().trim();
        System.out.print("Title: "); String title = sc.nextLine().trim();
        System.out.print("Author: "); String author = sc.nextLine().trim();
        System.out.print("Category: "); String category = sc.nextLine().trim();
        System.out.print("Publisher: "); String publisher = sc.nextLine().trim();
        System.out.print("Publish Year (e.g. 2020): "); int year = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Total Copies: "); int copies = Integer.parseInt(sc.nextLine().trim());

        Book book = bookService.addBook(isbn, title, author, category, publisher, Year.of(year), copies);
        System.out.println("Book added with ID: " + book.getBookId());
    }

    private void searchBooks() throws SQLException {
        System.out.print("Title (blank to skip): "); String title = sc.nextLine().trim();
        System.out.print("Author (blank to skip): "); String author = sc.nextLine().trim();
        System.out.print("Category (blank to skip): "); String category = sc.nextLine().trim();
        System.out.print("Only available? (y/n): "); Boolean onlyAvail = sc.nextLine().trim().equalsIgnoreCase("y");
        printBooks(bookService.search(title, author, category, onlyAvail));
    }

    private void printBooks(List<Book> books) {
        if (books.isEmpty()) { System.out.println("No books found."); return; }
        books.forEach(System.out::println);
        System.out.println("Total: " + books.size());
    }

    private void updateBook() throws SQLException, RecordNotFoundException {
        System.out.print("Book ID to update: "); int id = Integer.parseInt(sc.nextLine().trim());
        Book book = bookService.getBook(id);
        System.out.println("Current: " + book);
        System.out.print("New total copies (blank = no change): ");
        String copiesInput = sc.nextLine().trim();
        if (!copiesInput.isBlank()) {
            int newTotal = Integer.parseInt(copiesInput);
            int diff = newTotal - book.getTotalCopies();
            book.setTotalCopies(newTotal);
            book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + diff));
        }
        bookService.updateBook(book);
        System.out.println("Book updated.");
    }

    private void deleteBook() throws SQLException, RecordNotFoundException {
        System.out.print("Book ID to delete: "); int id = Integer.parseInt(sc.nextLine().trim());
        bookService.deleteBook(id);
        System.out.println("Book deleted.");
    }

    // ---------------- MEMBER MENU ----------------
    private void memberMenu() throws SQLException, LibraryException {
        System.out.println("\n-- Member Management --");
        System.out.println("1. Register  2. Search  3. List All  4. Suspend  0. Back");
        System.out.print("Choose: ");
        switch (sc.nextLine().trim()) {
            case "1" -> registerMember();
            case "2" -> searchMembers();
            case "3" -> printMembers(memberService.listAll());
            case "4" -> suspendMember();
            case "0" -> { }
            default -> System.out.println("Invalid choice.");
        }
    }

    private void registerMember() throws SQLException, DuplicateRecordException {
        System.out.print("Full Name: "); String name = sc.nextLine().trim();
        System.out.print("Email: "); String email = sc.nextLine().trim();
        System.out.print("Phone (10-15 digits): "); String phone = sc.nextLine().trim();
        System.out.print("Type (STUDENT/FACULTY/GENERAL): "); String type = sc.nextLine().trim().toUpperCase();
        Member m = memberService.registerMember(name, email, phone, Member.MemberType.valueOf(type));
        System.out.println("Member registered: " + m.getMembershipCode());
    }

    private void searchMembers() throws SQLException {
        System.out.print("Search by name/email: "); String q = sc.nextLine().trim();
        printMembers(memberService.search(q));
    }

    private void printMembers(List<Member> members) {
        if (members.isEmpty()) { System.out.println("No members found."); return; }
        for (Member m : members) {
            System.out.printf("[%d] %s | %s | %s | %s | Limit:%d | %s%n",
                    m.getMemberId(), m.getMembershipCode(), m.getFullName(),
                    m.getEmail(), m.getMemberType(), m.getMaxBooksAllowed(), m.getStatus());
        }
        System.out.println("Total: " + members.size());
    }

    private void suspendMember() throws SQLException, RecordNotFoundException {
        System.out.print("Member ID to suspend: "); int id = Integer.parseInt(sc.nextLine().trim());
        memberService.suspendMember(id);
        System.out.println("Member suspended.");
    }

    // ---------------- TRANSACTION MENU ----------------
    private void transactionMenu() throws SQLException, LibraryException {
        System.out.println("\n-- Issue / Return --");
        System.out.println("1. Issue Book  2. Return Book  3. Active Loans (by member)  0. Back");
        System.out.print("Choose: ");
        switch (sc.nextLine().trim()) {
            case "1" -> issueBook();
            case "2" -> returnBook();
            case "3" -> activeLoans();
            case "0" -> { }
            default -> System.out.println("Invalid choice.");
        }
    }

    private void issueBook() throws SQLException, RecordNotFoundException, BookNotAvailableException, MemberLimitExceededException {
        System.out.print("Book ID: "); int bookId = Integer.parseInt(sc.nextLine().trim());
        System.out.print("Member ID: "); int memberId = Integer.parseInt(sc.nextLine().trim());
        Transaction txn = txnService.issueBook(bookId, memberId, currentUser.getUserId());
        System.out.println("Issued! Transaction ID: " + txn.getTxnId() + ", Due: " + txn.getDueDate());
    }

    private void returnBook() throws SQLException, RecordNotFoundException {
        System.out.print("Transaction ID: "); long txnId = Long.parseLong(sc.nextLine().trim());
        Transaction txn = txnService.returnBook(txnId);
        System.out.println("Returned. Fine: " + txn.getFineAmount());
    }

    private void activeLoans() throws SQLException {
        System.out.print("Member ID: "); int memberId = Integer.parseInt(sc.nextLine().trim());
        List<Transaction> loans = txnService.activeLoansForMember(memberId);
        if (loans.isEmpty()) { System.out.println("No active loans."); return; }
        for (Transaction t : loans) {
            System.out.printf("Txn#%d | %s | Due: %s%n", t.getTxnId(), t.getBookTitle(), t.getDueDate());
        }
    }

    // ---------------- REPORTS MENU ----------------
    private void reportsMenu() throws SQLException {
        System.out.println("\n-- Reports --");
        System.out.println("1. Overdue Books  2. All Transactions  3. Stats  0. Back");
        System.out.print("Choose: ");
        switch (sc.nextLine().trim()) {
            case "1" -> {
                List<Transaction> overdue = txnService.overdueTransactions();
                if (overdue.isEmpty()) System.out.println("No overdue books.");
                overdue.forEach(t -> System.out.printf("Txn#%d | %s | %s | Due: %s%n",
                        t.getTxnId(), t.getBookTitle(), t.getMemberName(), t.getDueDate()));
            }
            case "2" -> {
                List<Transaction> all = txnService.allTransactions();
                all.forEach(t -> System.out.printf("Txn#%d | %s -> %s | %s | Fine: %s%n",
                        t.getTxnId(), t.getBookTitle(), t.getMemberName(), t.getStatus(), t.getFineAmount()));
            }
            case "3" -> {
                System.out.println("Total Books: " + bookService.totalBookCount());
                System.out.println("Total Members: " + memberService.totalMemberCount());
            }
            case "0" -> { }
            default -> System.out.println("Invalid choice.");
        }
    }
}
