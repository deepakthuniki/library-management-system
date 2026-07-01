-- Library Management System - Database Schema
-- Engine: InnoDB (required for ACID transactions / FK constraints)

CREATE DATABASE IF NOT EXISTS library_db;
USE library_db;

-- ========== USERS (Admin / Librarian login) ==========
CREATE TABLE IF NOT EXISTS users (
    user_id      INT AUTO_INCREMENT PRIMARY KEY,
    username     VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role         ENUM('ADMIN', 'LIBRARIAN') NOT NULL DEFAULT 'LIBRARIAN',
    full_name    VARCHAR(100) NOT NULL,
    created_at   TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

-- ========== BOOKS ==========
CREATE TABLE IF NOT EXISTS books (
    book_id        INT AUTO_INCREMENT PRIMARY KEY,
    isbn           VARCHAR(20)  NOT NULL UNIQUE,
    title          VARCHAR(200) NOT NULL,
    author         VARCHAR(150) NOT NULL,
    category       VARCHAR(80)  NOT NULL,
    publisher      VARCHAR(150),
    publish_year   YEAR,
    total_copies   INT NOT NULL DEFAULT 1,
    available_copies INT NOT NULL DEFAULT 1,
    created_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT chk_copies CHECK (available_copies >= 0 AND available_copies <= total_copies)
) ENGINE=InnoDB;

-- Indexes for fast filtering/search (title, author, category are the most
-- common search/filter columns -> composite + individual indexes)
CREATE INDEX idx_books_title    ON books(title);
CREATE INDEX idx_books_author   ON books(author);
CREATE INDEX idx_books_category ON books(category);
CREATE INDEX idx_books_isbn     ON books(isbn);

-- ========== MEMBERS ==========
CREATE TABLE IF NOT EXISTS members (
    member_id    INT AUTO_INCREMENT PRIMARY KEY,
    membership_code VARCHAR(20) NOT NULL UNIQUE,
    full_name    VARCHAR(150) NOT NULL,
    email        VARCHAR(150) NOT NULL UNIQUE,
    phone        VARCHAR(20)  NOT NULL,
    address      VARCHAR(255),
    member_type  ENUM('STUDENT','FACULTY','GENERAL') NOT NULL DEFAULT 'GENERAL',
    max_books_allowed INT NOT NULL DEFAULT 3,
    status       ENUM('ACTIVE','SUSPENDED','INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    joined_at    TIMESTAMP DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;

CREATE INDEX idx_members_name  ON members(full_name);
CREATE INDEX idx_members_email ON members(email);
CREATE INDEX idx_members_status ON members(status);

-- ========== ISSUE / RETURN TRANSACTIONS ==========
CREATE TABLE IF NOT EXISTS transactions (
    txn_id       BIGINT AUTO_INCREMENT PRIMARY KEY,
    book_id      INT NOT NULL,
    member_id    INT NOT NULL,
    issued_by    INT NOT NULL,                 -- FK -> users.user_id (librarian who issued)
    issue_date   DATE NOT NULL,
    due_date     DATE NOT NULL,
    return_date  DATE NULL,
    fine_amount  DECIMAL(8,2) NOT NULL DEFAULT 0.00,
    status       ENUM('ISSUED','RETURNED','OVERDUE','LOST') NOT NULL DEFAULT 'ISSUED',
    CONSTRAINT fk_txn_book   FOREIGN KEY (book_id)   REFERENCES books(book_id),
    CONSTRAINT fk_txn_member FOREIGN KEY (member_id) REFERENCES members(member_id),
    CONSTRAINT fk_txn_user   FOREIGN KEY (issued_by) REFERENCES users(user_id)
) ENGINE=InnoDB;

-- Composite index: the most frequent query is "active loans for a member"
-- and "active loans for a book" -> speeds up availability + overdue checks
CREATE INDEX idx_txn_member_status ON transactions(member_id, status);
CREATE INDEX idx_txn_book_status   ON transactions(book_id, status);
CREATE INDEX idx_txn_due_date      ON transactions(due_date);
CREATE INDEX idx_txn_status        ON transactions(status);

-- Seed default admin (password = "admin123", BCrypt hash generated at app first-run;
-- placeholder replaced by DataSeeder on first launch if table empty)
