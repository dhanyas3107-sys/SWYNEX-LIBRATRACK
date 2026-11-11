# LibraTrack – Library Management System

> **Core Java · OOP · Collections · Exception Handling · File Handling**
> Java Developer Internship – SWYNEX Technologies – **Task 2**

---

## 1. Project Overview

LibraTrack is a Core Java-based Library Management System developed to demonstrate practical implementation of Object-Oriented Programming concepts, Java Collections, exception handling and file handling. The application simplifies library operations by managing books, students, borrowing transactions, returns and overdue fines through an interactive console interface.

It uses **only the Java standard library** – no frameworks, no database, no external APIs. All data is stored in plain text files inside the `data/` folder, so the program remembers everything between runs.

## 2. Problem Statement

Small libraries often track books, members and loans on paper or in spreadsheets. This leads to duplicate IDs, books that are lent twice, lost borrowing history and fines that are calculated by hand (and inconsistently). A simple, reliable tool is needed that enforces the library's rules automatically and keeps a permanent record.

## 3. Objectives

- Apply the four OOP pillars (encapsulation, inheritance, abstraction, polymorphism) in a realistic domain.
- Use the Collections Framework (`ArrayList`, `HashMap`, `HashSet`) appropriately.
- Handle errors with `try-catch-finally`, `throw`, `throws` and custom exceptions, so bad input never crashes the program.
- Persist data with Java file I/O (`BufferedReader` / `BufferedWriter`) in a corruption-safe way.
- Use `LocalDate` and `ChronoUnit` for due dates and fines.
- Organise code in clean layers: models, services, storage, utilities, UI.

## 4. Key Features

| Area | What it does |
|---|---|
| **Book management** | Add, view, update and remove books; search by ID, title, author, or title + author; list available / borrowed books; total count. Unique IDs, validated ISBN/year, borrowed books cannot be removed. |
| **Student management** | Register and view students, search by ID, display user information, view all users (students + librarian) polymorphically. Unique user IDs. |
| **Issue books** | Checks that the student and book exist and the book is free; maximum **3 active books** per student; borrowing date and due date (**14 days**) set automatically. |
| **Return books** | Verifies the borrowing record, records the return date, frees the book and calculates the fine. |
| **Fine management** | **₹5 per overdue day**, calculated with `ChronoUnit.DAYS`; no fine on or before the due date. View fine for a transaction, overdue books with fines accrued so far, and a fine summary. |
| **Borrowing history** | All records, per-student history, active records, completed transactions. |
| **Statistics** | Books, availability, students, transactions, active loans and total fines. |
| **Persistence** | Data saved after every change; files and folder created automatically; special characters (`|`, `\`, quotes, Unicode) are safe; corrupt lines are skipped with a warning. |

## 5. Technologies Used

- **Java 17 or newer** (developed and tested on Java 21) – uses `record` and `switch` expressions
- **Java standard library only:** `java.util` collections, `java.time` (`LocalDate`, `ChronoUnit`), `java.nio.file` / `java.io` (file I/O), `java.util.regex`
- **Visual Studio Code** with the *Extension Pack for Java*
- **Git and GitHub** for version control

## 6. Project Architecture

The application is split into layers. Each layer only depends on the layers below it, so there are no circular dependencies:

```
        Main  (entry point – wires everything together)
          │
          ▼
        ui.ConsoleUI            ← menus, prompts, tables (no business rules)
          │
          ▼
   services.LibraryService      ← facade: one API for the UI + decides when to save
     │        │        │       │
     ▼        ▼        ▼       ▼
 BookService  StudentService  BorrowService ──► FineService
     │        │        │
     └────────┴────────┴──► models  (Book, User ▸ Student / Librarian, BorrowRecord)
                              ▲
   storage.FileStorage ───────┘   (reads/writes the data/ files)

   utils (InputValidator, LibraryConstants) and exceptions are used by every layer.
```

### How each major class works

| Class | Responsibility |
|---|---|
| `Main` | Creates `FileStorage` → `LibraryService`, loads data, starts `ConsoleUI`. Stops safely if data cannot be read (so it never overwrites your files with empty data). |
| `ConsoleUI` | All menus and prompts. Every action runs inside `runSafely(...)`, which turns any `LibraryException` into a friendly `[ERROR]` message. Input is read only with `nextLine()` and parsed by `InputValidator`. Handles closed input (Ctrl+D) by saving and exiting. |
| `LibraryService` | Facade over the services. Loads data at start-up, re-derives each book's availability from the borrowing records, saves the changed files after each operation, builds statistics. Holds the default `Librarian`. |
| `BookService` | Book catalogue: `ArrayList<Book>` + `HashMap<Integer, Book>` kept in sync. Add, update (validate-then-apply), remove, three overloaded `searchBooks` methods. |
| `StudentService` | Registered students: `ArrayList<Student>` + `HashMap<Integer, Student>`. |
| `BorrowService` | `issueBook` and `returnBook` (each with an overload taking an explicit date), enforces the borrowing limit, owns the `ArrayList<BorrowRecord>` and the transaction-ID counter. |
| `FineService` | The one reusable place for overdue-day and fine calculation (`calculateOverdueDays`, `calculateFine`). |
| `FileStorage` | Pipe-delimited text files with escaping, per-line error tolerance, atomic saves (temp file + move). |
| `InputValidator` | Validation/parsing of IDs, text, e-mail, phone, ISBN, years, ints and dates. |
| `LibraryConstants` | Fine per day, loan period, borrowing limit, file names, default librarian – nothing is hard-coded elsewhere. |
| `Book`, `User`, `Student`, `Librarian`, `BorrowRecord` | Domain models with validated state (see the OOP section). |
| `LibraryStatistics` | Immutable `record` carrying the statistics numbers. |

## 7. Folder Structure

```
LibraTrack/
├── src/
│   ├── Main.java
│   ├── models/
│   │   ├── Book.java
│   │   ├── User.java              (abstract)
│   │   ├── Student.java
│   │   ├── Librarian.java
│   │   └── BorrowRecord.java
│   ├── services/
│   │   ├── LibraryService.java
│   │   ├── BookService.java
│   │   ├── StudentService.java
│   │   ├── BorrowService.java
│   │   ├── FineService.java
│   │   └── LibraryStatistics.java
│   ├── exceptions/
│   │   ├── LibraryException.java              (base class)
│   │   ├── BookNotAvailableException.java
│   │   ├── BookNotFoundException.java
│   │   ├── StudentNotFoundException.java
│   │   ├── BorrowingLimitExceededException.java
│   │   ├── InvalidInputException.java
│   │   ├── DuplicateRecordException.java
│   │   ├── OperationNotAllowedException.java
│   │   ├── BorrowRecordNotFoundException.java
│   │   └── StorageException.java
│   ├── storage/
│   │   └── FileStorage.java
│   ├── ui/
│   │   └── ConsoleUI.java
│   └── utils/
│       ├── InputValidator.java
│       └── LibraryConstants.java
├── test/
│   └── LibraTrackTests.java       (automated tests – no JUnit needed)
├── data/                          (sample data; recreated automatically if missing)
│   ├── books.txt
│   ├── students.txt
│   └── borrow_records.txt
├── run.sh / run.bat               (compile + run helpers)
├── README.md
└── .gitignore
```

The five required exceptions are all present; `LibraryException`, `DuplicateRecordException`, `OperationNotAllowedException`, `BorrowRecordNotFoundException` and `StorageException` were added so every error situation has a precise, meaningful type. `ui/ConsoleUI.java` was added so that `Main.java` stays tiny.

## 8. The Four OOP Pillars – and Where to Find Them

### A. Encapsulation – `Book`, `User`, `Student`, `Librarian`, `BorrowRecord`
- Every attribute is `private`.
- Setters **validate before changing state**: `Book.setIsbn(...)` calls `InputValidator.validateIsbn`, `User.setEmail(...)` validates the e-mail format, etc. A rejected value leaves the object unchanged.
- Sensitive data has **no setter**: `Book.bookId`, `User.userId`, and all of `BorrowRecord`'s identifiers and dates are `final`.
- State changes are controlled: a book's availability can only change through `Book.markBorrowed()` (which refuses if already borrowed) and `Book.markReturned()`; a transaction can only be closed through `BorrowRecord.markReturned(date, fine)`, which rejects double returns and impossible dates.
- Collections are exposed read-only (`Collections.unmodifiableList`) so the UI cannot modify them directly.

### B. Inheritance – `User` → `Student`, `Librarian`
- `User` holds the common fields (`userId`, `name`, `email`, `contactNumber`) and their validation.
- `Student` adds `department` and `yearOfStudy`; `Librarian` adds `employeeId` and `designation`.
- Both call `super(...)` in their constructors and extend `getDetails()` with `super.getDetails() + ...`, so the common code is never duplicated.

### C. Abstraction – abstract class `User`
- `User` is `abstract` with three abstract methods that every user type must implement: `displayRole()`, `getUserType()` and `getBorrowLimit()`.
- The abstraction is functional, not decorative: `BorrowService.issueBook` enforces `student.getBorrowLimit()`, `ConsoleUI` prints `displayRole()`, and the borrow limit is **3 for a `Student` and 0 for a `Librarian`**.

### D. Polymorphism
- **Method overriding (runtime polymorphism):** `Student.displayRole()` and `Librarian.displayRole()` return different text; `getUserType()`, `getBorrowLimit()` and `getDetails()` are overridden too.
- **Parent-class references:** `LibraryService.getAllUsers()` returns a `List<User>` containing both students and the librarian. In the app, choose **Student Management → 4 “Display all library users and roles”** – `ConsoleUI.showAllUsers()` loops over `User` references and the correct `Student` / `Librarian` version of each method runs.
- **Method overloading (compile-time polymorphism):** `BookService.searchBooks(int bookId)`, `searchBooks(String keyword, SearchBy field)` (title or author) and `searchBooks(String title, String author)`. Also overloaded: `BorrowService.issueBook(...)` / `returnBook(...)` (with or without an explicit date), `FineService.calculateFine(LocalDate, LocalDate)` / `calculateFine(BorrowRecord, LocalDate)`, and the constructors of `Book` and `BorrowRecord`.

## 9. Java Collections Used

| Collection | Where | Why |
|---|---|---|
| `ArrayList<Book>` | `BookService.books` | Keeps books in insertion order for listings. |
| `HashMap<Integer, Book>` | `BookService.bookMap` | O(1) lookup and duplicate-ID check by book ID. |
| `ArrayList<Student>` | `StudentService.students` | Ordered list of registered students. |
| `HashMap<Integer, Student>` | `StudentService.studentMap` | O(1) lookup and duplicate-ID check by student ID. |
| `ArrayList<BorrowRecord>` | `BorrowService.records` | Complete, ordered transaction history. |
| `HashSet<Integer>` | `BorrowService.restoreRecords` | Detects duplicate transaction IDs while loading. |
| `List.of`, `Collections.unmodifiableList` | services | Read-only views so outside code cannot corrupt the data. |

The list and the map in each service are always updated together (`addBook` adds to both, `removeBook` removes from both), so they cannot drift apart. Records are **not duplicated**: availability is stored once on `Book`, and a `BorrowRecord` stores only IDs plus a snapshot of the names/titles needed for history.

## 10. Exception Handling Implementation

| Situation | Exception | Message example |
|---|---|---|
| Invalid menu choice / non-numeric input / bad date / bad e-mail, ISBN, year… | `InvalidInputException` | `Invalid menu choice 99. Please enter a number from 1 to 8.` |
| Duplicate book or student ID | `DuplicateRecordException` | `A book with ID 9 already exists. Book IDs must be unique.` |
| Unknown book ID | `BookNotFoundException` | `No book found with ID 999.` |
| Unregistered student | `StudentNotFoundException` | `No student is registered with ID 999.` |
| Issuing a book that is out | `BookNotAvailableException` | `"Effective Java" (Book ID 2) is not available - it is already issued.` |
| 4th active book | `BorrowingLimitExceededException` | `Student 103 already has 3 active book(s), which is the maximum allowed.` |
| Returning a book never borrowed | `BorrowRecordNotFoundException` | `No active borrowing record found for student 104 and book 1…` |
| Removing a borrowed book | `OperationNotAllowedException` | `…is currently borrowed and cannot be removed. Return it first.` |
| File read/write failure | `StorageException` | `Could not write 'books.txt': …` |

How it fits together:
- All of the above extend one checked base class, `LibraryException`, so the UI can catch them in one place.
- Model constructors and setters `throw` them; services declare them with `throws`; `ConsoleUI.runSafely()` catches them and prints `[ERROR] <message>` – never a stack trace.
- `FileStorage.writeAtomically` and `ConsoleUI.run` use **`finally`** (clean up the temp file; always save on exit).
- `InputClosedException` (private to the UI) handles Ctrl+D / end of input gracefully.
- A last-resort `catch (RuntimeException)` in `runSafely` prevents any unexpected bug from terminating the program.

## 11. File Handling and Data Persistence

Three UTF-8 text files in `data/`, one record per line, fields separated by `|`:

```
books.txt          bookId|title|author|category|isbn|publicationYear|available
students.txt       studentId|name|email|contact|department|yearOfStudy
borrow_records.txt transactionId|studentId|studentName|bookId|bookTitle|borrowDate|dueDate|returnDate|fineAmount|status
```

- **Load on start-up / save on change:** books are saved after add/update/remove; students after registration; books + records after every issue/return; everything again on exit.
- **Automatic creation:** a missing `data/` folder or file is created (with a header comment line); a missing file simply means “no data yet”.
- **Special characters are safe:** inside a field, `\` is written `\\`, `|` is written `\p`, and line breaks `\n` / `\r`. A title such as `Tricky | Title \ "quotes"` round-trips exactly (covered by a test).
- **Corruption-safe saves:** data is first written to `<file>.tmp` and then moved over the real file (atomic move where supported), so a crash mid-save cannot leave a half-written file.
- **Tolerant loading:** a malformed line is skipped and reported as a `[WARNING]` at start-up; the rest still loads. If a file cannot be read at all (e.g. not UTF-8) the program stops with a clear message instead of overwriting it.
- **Availability is restored correctly:** after loading, each book's availability is recomputed from the *active* borrowing records, so the files can never disagree with each other. If a repair was needed the user is told.
- **History is preserved:** returned transactions stay in `borrow_records.txt` forever; the next transaction ID continues after the highest stored one.

## 12. Application Workflow

```
Start → create data files if missing → load books, students, records
      → reconcile availability → show main menu
Main menu ─┬─ 1 Book Management ─── add / view / search / update / remove / lists / count
           ├─ 2 Student Management ─ register / view / search / all users (polymorphism)
           ├─ 3 Issue a Book ─────── student ✔ → book ✔ → available ✔ → limit ✔ → record + save
           ├─ 4 Return a Book ────── record ✔ → return date → fine → free book + save
           ├─ 5 Borrowing History ── all / by student / active / completed
           ├─ 6 Fine Management ──── fine for a transaction / overdue list / summary
           ├─ 7 Library Statistics
           └─ 8 Exit ─────────────── save all data → goodbye
```

## 13. Installation

1. Install a **JDK 17 or newer** (check with `java -version` and `javac -version`).
2. Install **Visual Studio Code** and the **Extension Pack for Java** (optional but recommended).
3. Download or clone this project and open the `LibraTrack` folder.

No other software, database or internet connection is needed.

## 14. How to Compile and Run

**Always run from the project root (the folder that contains `src/` and `data/`)** so the `data/` folder is found.

### Quick scripts
- Linux / macOS / Git Bash: `./run.sh`  (tests: `./run.sh test`)
- Windows Command Prompt: `run.bat`  (tests: `run.bat test`)

### Manual commands

**Linux / macOS / Git Bash**
```bash
mkdir -p out
javac -encoding UTF-8 -d out $(find src -name "*.java")
java -cp out Main
```

**Windows PowerShell**
```powershell
mkdir out -Force
javac -encoding UTF-8 -d out (Get-ChildItem -Recurse src -Filter *.java).FullName
java -cp out Main
```

### In Visual Studio Code
1. `File → Open Folder…` → choose `LibraTrack`.
2. Open `src/Main.java` and click **Run** above `main`, or press `F5`.
3. Use the integrated terminal for input. (The automated tests live in `test/`, so run them from the terminal: `./run.sh test` or `run.bat test`.)

> **₹ symbol shows as “?” on Windows?** Run `chcp 65001` first (`run.bat` does this for you), use Windows Terminal, or change `CURRENCY_SYMBOL` in `utils/LibraryConstants.java` to `"Rs. "`.

## 15. Sample Console Output

Output captured from a real run against the bundled sample data (dates will differ on other days).

**Start-up and errors**
```
=============================================
               LIBRATRACK
        LIBRARY MANAGEMENT SYSTEM
=============================================

Welcome to LibraTrack!
Librarian on duty: Anitha Raman

=============================================
MAIN MENU
=============================================
1. Book Management
2. Student Management
3. Issue a Book
4. Return a Book
5. Borrowing History
6. Fine Management
7. Library Statistics
8. Exit

Enter your choice: abc
[ERROR] Menu choice must be a whole number (you entered 'abc').
Enter your choice: 99
[ERROR] Invalid menu choice 99. Please enter a number from 1 to 8.
```

**Issuing and a rejected 4th book**
```
ISSUE A BOOK
Student ID: 102
Book ID: 4
[SUCCESS] Book issued successfully.
  Transaction               : TXN-1005
  Student                   : Priya Sharma (ID 102)
  Book                      : Head First Java (ID 4)
  Borrowed on               : 2026-11-11
  Due date                  : 2026-11-25
  Late fine                 : ₹5 per day after the due date

Student ID: 103
Book ID: 6
[ERROR] Student 103 already has 3 active book(s), which is the maximum allowed. Please return a book first.
```

**Returning an overdue book (8 days late → ₹40)**
```
RETURN A BOOK
Student ID: 101
Book ID: 2
Return date yyyy-MM-dd (press Enter for today):
[SUCCESS] Book returned successfully.
---------------------------------------------
  Transaction               : TXN-1003 (RETURNED)
  Student                   : Arjun Kumar (ID 101)
  Book                      : Effective Java (ID 2)
  Borrowing date            : 2026-10-20
  Due date                  : 2026-11-03
  Return date               : 2026-11-11
  Days overdue              : 8
  Fine charged              : ₹40
---------------------------------------------
```

**Book table and polymorphism demo**
```
ID    Title                      Author             Category       ISBN          Year Status
-----------------------------------------------------------------------------------------------
2     Effective Java             Joshua Bloch       Programming    9780134685991 2018 Borrowed
4     Head First Java            Kathy Sierra       Programming    9781491910771 2022 Available
-----------------------------------------------------------------------------------------------
2 book(s) listed.

[LIBRARIAN] Anitha Raman
   Role   : Head Librarian - manages books, members, issues, returns and fines
   Borrow limit: 0
[STUDENT] Arjun Kumar
   Role   : Student of Computer Science (Year 2) - may borrow up to 3 books
   Borrow limit: 3
```

**Statistics**
```
LIBRARY STATISTICS
  Total books                     : 9
  Available books                 : 6
  Borrowed books                  : 3
  Registered students             : 5
  Total borrowing transactions    : 7
  Active borrowing transactions   : 3
  Total fines generated           : ₹255
```

## 16. Testing

### 16.1 Automated tests (81 checks, no extra libraries)
```bash
./run.sh test        # or:  java -cp out LibraTrackTests   (after compiling)
```
`test/LibraTrackTests.java` is a small self-contained test runner (no JUnit jar needed, so the project stays “Core Java only”). Each scenario runs against a temporary data folder, so your real `data/` files are never touched. Expected final line: `Passed: 81   Failed: 0`.

### 16.2 Manual test scenarios

Start each manual session from the **bundled sample data** (re-copy the `data/` folder to reset it). Sample data: books 1–8 (books **2** and **5** are on loan), students **101–104**, transactions TXN-1001 to TXN-1004; Arjun (101) holds book 2 due **2026-11-03**, Rahul (103) holds book 5 due **2026-11-19**.

| # | Scenario | Steps (menu path) | Expected result |
|---|---|---|---|
| 1 | Add a new book | Book Mgmt → 1: ID `9`, “The C++ Programming Language”, “Bjarne Stroustrup”, “Programming”, ISBN `9780321563842`, year `2013` | `[SUCCESS] Book added`; total books = 9 |
| 2 | Duplicate book | Book Mgmt → 1: ID `1` | `[ERROR] A book with ID 1 already exists…`; nothing added |
| 3 | Register a student | Student Mgmt → 1: `105`, “Ananya Rao”, `ananya.rao@example.com`, `9876501234`, “Civil”, `2` | `[SUCCESS] Student registered`; appears in “View all students” |
| 4 | Duplicate student | Student Mgmt → 1: ID `101` | `[ERROR] User ID 101 is already in use…` |
| 5 | Search books | Book Mgmt → 4: `java`; → 5: `bloch`; → 3: `3` | 2 books (IDs 2, 4); 1 book (ID 2); “Introduction to Algorithms” |
| 6 | Issue an available book | Issue → student `102`, book `4` | `[SUCCESS]`; due date = today + 14 days; book 4 shows *Borrowed* |
| 7 | Issue an unavailable book | Issue → student `102`, book `2` | `[ERROR] "Effective Java" (Book ID 2) is not available…` |
| 8 | Borrowing limit | Issue → `103`/book `1`, `103`/book `3` (both succeed – 103 already holds book 5), then `103`/book `6` | Third request: `[ERROR] Student 103 already has 3 active book(s)…` |
| 9 | Return a borrowed book | Return → student `103`, book `1`, Enter for today | `[SUCCESS]`; fine ₹0; book 1 *Available* again |
| 10 | Return a never-borrowed book | Return → student `104`, book `1` | `[ERROR] No active borrowing record found for student 104 and book 1…` |
| 11 | Overdue fine | Return → student `101`, book `2`, date `2026-11-13` | Due 2026-11-03 → **10 days overdue → ₹50** |
| 12 | Return before due date | Return → student `103`, book `5`, date `2026-11-10` | Due 2026-11-19 → days overdue 0, fine ₹0 |
| 13 | Saving | After any of the above, open `data/*.txt` | The change is in the file immediately (before exiting) |
| 14 | Restart & reload | Exit (option 8), run again → Statistics / History | All books, students, transactions and fines are still there; borrowed books still show *Borrowed* |
| 15 | Update & delete | Book Mgmt → 6: book `4`, new title, Enter to keep the rest; → 7: book `6` (confirm `y`); → 7: book `2` while borrowed | Update succeeds and persists; book 6 removed; removing a borrowed book → `[ERROR] …cannot be removed. Return it first.` |

Extra checks worth trying: enter letters for a menu choice, an invalid date such as `31/12/2026`, an e-mail without `@`, a 9-digit ISBN, press **Ctrl+D** at any prompt (data is saved and the program exits cleanly), delete the whole `data/` folder (it is recreated), or corrupt one line of `students.txt` (that line is skipped with a warning).

> **Trying fines without waiting 14 days:** the return screen accepts an optional return date (`yyyy-MM-dd`) so you can demonstrate overdue fines immediately (scenario 11).

## 17. Future Enhancements

- Student login and a librarian login with passwords (hashed).
- Book reservations / waiting list and renewals.
- Multiple copies per title with per-copy tracking.
- Export reports to CSV/PDF; e-mail reminders for due dates.
- Replace the text files with a database (JDBC + SQLite/MySQL) behind the same `FileStorage`-style interface.
- Add JUnit 5 tests and a GitHub Actions build.
- Swing/JavaFX GUI or a REST API on top of the existing `LibraryService`.

## 18. Uploading to GitHub

```bash
cd LibraTrack
git init
git add .
git commit -m "LibraTrack: Core Java library management system"
git branch -M main
# create an empty repository named LibraTrack on github.com first, then:
git remote add origin https://github.com/<your-username>/LibraTrack.git
git push -u origin main
```
The `.gitignore` already excludes compiled `out/` files and IDE folders; the sample `data/` files are included so the project runs immediately after cloning.

## 19. Author and Internship Information

- **Project:** LibraTrack – Library Management System
- **Author:** Ashfaq Muhammed A – B.E. Computer Science & Engineering, Chennai Institute of Technology
- **Internship:** Java Developer Internship, **SWYNEX Technologies**
- **Task:** Task 2 – Core Java Library Management System (OOP, Collections, Exception Handling, File Handling)
- **Language / version:** Java 17+ (tested on Java 21)
