
# Library Management System

A console-based Library Management System built with **Java 17, JDBC, and MySQL**,
demonstrating OOP design, ACID-compliant transaction handling, and optimized SQL.

Built to match the resume bullets:
> Designed and implemented console-based Library Management System supporting book
> cataloging, member registration, and dynamic issue/return tracking for 500+ records.
> Implemented JDBC connectivity to MySQL ensuring ACID compliance using commit/rollback
> mechanisms. Applied OOP principles (inheritance, encapsulation, polymorphism) reducing
> code duplication by 40%. Developed optimized SQL queries with indexing for efficient
> data retrieval and complex filtering.

---

## 1. Features

- **Book catalog**: add, search (by title/author/category), update, delete, list
- **Member registration**: STUDENT / FACULTY / GENERAL types with per-type loan limits
- **Issue / Return workflow**: atomic transactions with automatic due-date and fine
  calculation (₹5/day overdue)
- **Role-based login**: ADMIN and LIBRARIAN accounts, BCrypt-hashed passwords
- **Reports**: overdue books, full transaction history, catalog/member stats
- **Seed data**: 500 books + 120 members auto-generated on first run

## 2. Tech Stack

| Layer          | Choice                          | Why |
|----------------|----------------------------------|-----|
| Language        | Java 17                         | Matches resume requirement; modern language features (records-style switch, text blocks) |
| DB Access       | Raw JDBC + HikariCP pooling     | Resume explicitly calls out JDBC; HikariCP is the industry-standard pool used in production Java apps instead of hand-rolled connection management |
| Database        | MySQL 8.0                       | Resume requirement; InnoDB engine for full ACID transactions |
| Build tool      | Maven                           | Industry standard, dependency + build lifecycle management |
| Password hashing| jBCrypt                         | Never store plaintext passwords |
| Logging         | SLF4J + Logback                 | Production-grade structured logging instead of `System.out` |
| Testing         | JUnit 4 + Mockito                | Unit tests (mocked DAO) + real integration tests against MySQL |

## 3. Architecture

Clean layered architecture — each layer only talks to the one below it:

```
ui/            Console menus (ConsoleUI) - no business logic
   |
service/       Business rules: validation, loan limits, fine calculation
   |
dao/           JDBC data access - SQL lives here only
   |
model/         POJOs: Book, Member, User, Transaction (+ Person base class)
   |
util/          DatabaseConnection (HikariCP), PasswordUtil, ValidationUtil, DataSeeder
   |
exception/     Custom checked exceptions (LibraryException hierarchy)
```

### OOP principles applied
- **Inheritance**: `Member` and `User` both extend abstract `Person`
- **Polymorphism**: `Person.describeRole()` is overridden differently by `Member` (membership type) and `User` (staff role)
- **Encapsulation**: `Book.borrowCopy()`/`returnCopy()` are the *only* way to mutate `availableCopies`, enforcing the invariant `0 <= available <= total` from inside the object itself
- **Interface abstraction**: `Dao<T, ID>` generic contract, implemented by `BookDao`, `MemberDao`, `UserDao` to avoid duplicating CRUD method signatures

### ACID compliance (commit/rollback)
`TransactionDao.issueBook()` and `.returnBook()` each wrap **two writes** (the
`transactions` insert/update AND the `books.available_copies` update) in a single
JDBC transaction:

```java
conn.setAutoCommit(false);
try {
    // insert transaction row
    // update books.available_copies
    conn.commit();          // both succeed together
} catch (SQLException e) {
    conn.rollback();        // both undone together
    throw e;
}
```

This guarantees the book count and loan records can never drift out of sync, even
if the second write fails.

### Database schema & indexing

See [`sql/schema.sql`](sql/schema.sql). Key indexes:
- `books`: indexes on `title`, `author`, `category`, `isbn` — the columns used in
  `BookDao.search()`'s dynamic `WHERE` clause
- `transactions`: composite indexes on `(member_id, status)` and `(book_id, status)`
  for fast "active loans" lookups, plus `due_date` for overdue queries
- Foreign keys enforce referential integrity between `transactions`, `books`, `members`, `users`

## 4. Project Structure

```
library-management-system/
├── pom.xml
├── docker-compose.yml
├── docker/Dockerfile
├── sql/schema.sql
├── .github/workflows/ci.yml
├── src/main/java/com/library/
│   ├── Main.java
│   ├── model/        Book, Member, User, Transaction, Person
│   ├── dao/           BookDao, MemberDao, UserDao, TransactionDao, Dao<T,ID>
│   ├── service/        BookService, MemberService, AuthService, TransactionService
│   ├── ui/              ConsoleUI
│   ├── util/            DatabaseConnection, PasswordUtil, ValidationUtil, DataSeeder
│   └── exception/       LibraryException + subclasses
├── src/main/resources/  db.properties, logback.xml
└── src/test/java/com/library/
    ├── model/BookTest.java                          (unit)
    ├── service/BookServiceTest.java                 (unit, Mockito-mocked DAO)
    ├── util/ValidationUtilTest.java                 (unit)
    ├── util/PasswordUtilTest.java                   (unit)
    └── dao/TransactionDaoIntegrationTest.java        (integration, real MySQL)
```

## 5. Setup & Run

### Prerequisites
- JDK 17+
- Maven 3.8+
- MySQL 8.0 running locally (or use Docker Compose, see below)

### Option A — Local MySQL

```bash
# 1. Create database + apply schema
mysql -u root -p < sql/schema.sql

# 2. (Optional) create a dedicated app user
mysql -u root -p -e "
  CREATE USER 'library_app'@'localhost' IDENTIFIED BY 'LibraryApp@2026';
  GRANT ALL PRIVILEGES ON library_db.* TO 'library_app'@'localhost';
  FLUSH PRIVILEGES;"

# 3. Build
mvn clean package

# 4. Run (first run auto-seeds 500 books + 120 members + admin/librarian accounts)
java -jar target/library-management-system.jar
```

**Default login credentials** (seeded automatically):
| Username  | Password  | Role      |
|-----------|-----------|-----------|
| admin     | admin123  | ADMIN     |
| librarian | lib123    | LIBRARIAN |

> Change these immediately in any non-demo deployment.

### Option B — Docker Compose (app + MySQL together)

```bash
docker compose run --rm app
```

This builds the image, starts MySQL, waits for its healthcheck, applies
`sql/schema.sql` automatically via the MySQL init-scripts mechanism, then
launches the console app attached to your terminal.

### Environment variable overrides

`DB_URL`, `DB_USER`, `DB_PASSWORD` override `src/main/resources/db.properties`
at runtime — used by Docker Compose and CI.

## 6. Testing

```bash
# All tests (unit + integration; requires MySQL reachable per db.properties)
mvn test

# Just the fast unit tests, skip the DB-backed integration test
mvn test -Dtest='!TransactionDaoIntegrationTest'
```

- **Unit tests** (`BookTest`, `BookServiceTest`, `ValidationUtilTest`, `PasswordUtilTest`)
  run with no database — `BookServiceTest` mocks `BookDao` with Mockito to test
  validation/duplicate-detection rules in isolation.
- **Integration test** (`TransactionDaoIntegrationTest`) runs against a real MySQL
  instance and specifically verifies the ACID guarantee: that `issueBook`/`returnBook`
  either fully commit (transaction row + copy count both update) or fully roll back
  (no phantom transaction rows left behind on failure).

All 26 tests pass as of the last verified run (23 unit + 3 integration).

## 7. CI/CD

`.github/workflows/ci.yml` runs on every push/PR to `main`:
1. Spins up a real MySQL 8.0 service container
2. Applies `sql/schema.sql`
3. Builds the JAR with Maven
4. Runs the full test suite (unit + integration) against that MySQL instance
5. Uploads the built JAR and test reports as workflow artifacts

## 8. Known Limitations

- Single-process console app: the loan-limit check in `TransactionService.issueBook()`
  reads the active-loan count and then writes in a separate step — fine for a
  single-user console tool, but a concurrent multi-user (web) version would need
  that check inside the same DB transaction with row locking to avoid a race.
- No password reset / account lockout flow (3 failed console login attempts just
  exits the app).
- Fine amount is a flat rate; no fine caps or waiver workflow.
- No pagination on `list all books/members` — fine at hundreds of rows, would need
  it at real scale.

## 9. Possible Next Steps

- Add a `ReportService` that exports overdue-book reports to CSV
- Add pagination + a proper interactive TUI (e.g. `lanterna`) instead of raw `Scanner`
- Extract the loan-limit check into the same transaction as `issueBook` using
  `SELECT ... FOR UPDATE` for correctness under concurrency
- Add Flyway/Liquibase for schema migrations instead of a single `schema.sql`
=======
