# Vertex Banking: Customer Account & Transaction Management System
> **"Vertex Bank - Smart Banking, Simplified"**  
> **Course:** Programming in JAVA (25CSE0202) — Project Evaluation (50 Marks)  
> **Group:** G2 | **Team ID:** G02-PID-17  

---



## 1. Problem Statement & Proposed Solution

### Problem
Traditional banking academic setups and legacy financial prototypes often manage customers, accounts, and transactions across unvalidated spreadsheets and flat files. This leads to data redundancy, race conditions during simultaneous withdrawals, inaccurate balances due to floating-point rounding errors, lack of transaction atomicity, and poor reporting capabilities.

### Proposed Solution
**Vertex Banking** is a robust, modular Java console application built on a layered architecture (Presentation $\to$ Service $\to$ DAO/Repository $\to$ Database). It provides:
1. **Strict Financial Accuracy:** Exclusively employs `BigDecimal` for zero floating-point drift.
2. **Encapsulated OOP Model & Custom Exceptions:** Enforces non-negative balances, minimum opening deposits (₹1,000 for Savings, ₹5,000 for Current), customer age verification ($\ge 18$ years), and account status lifecycles (`ACTIVE`, `FROZEN`, `CLOSED`).
3. **Thread-Safe Concurrency Control:** Guards balances against race conditions using synchronized critical sections (`BalanceManager`), preventing overdrawing even under simultaneous multi-threaded load.
4. **Producer-Consumer & Executor Framework:** Bounded task queues using intrinsic monitors (`wait()` / `notifyAll()`) and `ExecutorService` thread pooling with `Callable` and `Future`.
5. **ACID Relational Persistence:** Complete JDBC persistence with `PreparedStatement`, `ResultSet`, connection pooling, and multi-step transaction rollback upon failure.
6. **Stream API Analytics:** Generates real-time banking analytics using functional interfaces, `groupingBy`, `counting`, and summary collectors.
7. **Generic Repositories & File I/O:** Generic `Repository<T>` interface and robust CSV import/export with line-by-line error audit logging.

---

## 2. Architecture & Package Structure

```
Console UI (VertexBankingApp)
          |
          v
Service Layer (CustomerService, AccountService, TransferService, ReportService)
          |
     +----+----+
     |         |
     v         v
DAO Layer   Generic In-Memory Repository
     |         (Repository<T>, InMemoryRepository<T>)
     v
JDBC Layer (DBConnection, PreparedStatement, ResultSet)
     |
     v
Relational Database (MySQL 8.0+ / Built-in Embedded JDBC Engine)
```

### Source Tree Breakdown
```
D:\gg\vertex_banking\
├── src\
│   ├── model\
│   │   ├── Customer.java               # Customer profile with age and regex validation
│   │   ├── Account.java                # Account entity implementing Withdrawable (BigDecimal)
│   │   ├── Transaction.java            # Immutable balance movement audit record
│   │   ├── AccountType.java            # Enum: SAVINGS (Min ₹1,000), CURRENT (Min ₹5,000)
│   │   ├── AccountStatus.java          # Enum: ACTIVE, FROZEN, CLOSED
│   │   ├── TransactionType.java        # Enum: DEPOSIT, WITHDRAWAL, TRANSFER_IN, TRANSFER_OUT
│   │   ├── TransactionStatus.java      # Enum: SUCCESS, FAILED, REJECTED
│   │   └── Withdrawable.java           # Abstraction contract for funds release
│   ├── exception\
│   │   ├── VertexException.java        # Checked base application exception
│   │   ├── InvalidCustomerException.java
│   │   ├── InvalidAccountException.java
│   │   ├── InsufficientFundsException.java
│   │   ├── DuplicateRecordException.java
│   │   └── TransactionException.java
│   ├── repository\
│   │   ├── Repository.java             # Generic CRUD repository contract (Repository<T>)
│   │   └── InMemoryRepository.java     # Thread-safe generic ConcurrentHashMap repository
│   ├── database\
│   │   ├── DBConnection.java           # JDBC Connection Manager with MySQL & Fallback
│   │   └── SimulatedJdbcDriver.java    # Embedded In-Memory JDBC Engine (PreparedStatement, Rollback)
│   ├── dao\
│   │   ├── CustomerDAO.java            # JDBC CRUD for customers
│   │   ├── AccountDAO.java             # JDBC CRUD for accounts with transactional balance updates
│   │   └── TransactionDAO.java         # JDBC audit trail persistence
│   ├── service\
│   │   ├── CustomerService.java        # Business rules for registration and duplicate checking
│   │   ├── AccountService.java         # Deposits, withdrawals, limits, status transitions
│   │   ├── TransferService.java        # Atomic fund transfers with commit and rollback
│   │   └── ReportService.java          # Analytics using Java Stream API & Lambdas
│   ├── concurrency\
│   │   ├── BalanceManager.java         # Synchronized critical section guarding account balance
│   │   ├── TransactionTask.java        # Runnable withdrawal simulation task
│   │   ├── TransactionQueue.java       # Monitor-coordinated bounded queue (wait / notifyAll)
│   │   ├── TransactionProducer.java    # Work enqueueing thread
│   │   ├── TransactionConsumer.java    # Work processing thread
│   │   └── SettlementCalculatorTask.java # Callable<BigDecimal> batch aggregator
│   ├── util\
│   │   ├── InputValidator.java         # Regex and date format validator
│   │   ├── CsvImporter.java            # BufferedReader CSV parser with line-by-line rejection log
│   │   └── CsvExporter.java            # BufferedWriter CSV report generator
│   ├── test\
│   │   └── VertexTestRunner.java       # Automated verification engine for tests T01 - T18
│   └── main\
│       └── VertexBankingApp.java       # Interactive CLI master menu application
├── sql\
│   └── vertex_banking_schema.sql       # Relational DDL script with constraints and demo dataset
├── data\
│   ├── customers.csv                   # Sample input CSV with valid and malformed rows
│   └── test_exported_accounts.csv      # Exported CSV records
├── compile.bat                         # Windows compilation batch script
├── run.bat                             # Windows CLI application launch script
├── run-tests.bat                       # Windows automated test execution script
└── README.md                           # Documentation, diagrams, and viva guide
```

---

## 3. Eight Development Milestones (50 Marks Mapping)

| Milestone | Marks | Requirements | Implemented Artifacts & Evidence |
|:---|:---:|:---|:---|
| **1. OOP Model, String & Exceptions** | **8** | Encapsulated models, `Withdrawable` interface, `BigDecimal` money, custom exceptions, validation. | `Customer.java`, `Account.java`, `Withdrawable.java`, `VertexException` hierarchy, `BigDecimal` scale handling. |
| **2. Collections & Record Management** | **6** | `HashMap` for $O(1)$ lookups, `HashSet` for unique email tracking, `ArrayList`, sorting with `Comparator`. | `CustomerService.registeredEmails` (`HashSet`), `InMemoryRepository` (`Map`), `AccountService.getTopAccounts` (`Comparator`). |
| **3. Multithreading & Synchronization** | **7** | `BalanceManager` with `synchronized` critical section; 5 threads competing for ₹2,000 balance $\to$ exactly 2 succeed. | `BalanceManager.java`, `TransactionTask.java`, Test `T10` outputting thread names and never driving balance negative. |
| **4. Producer-Consumer & Executor** | **6** | Bounded queue with `wait()`/`notifyAll()` in `while` loop; `ExecutorService`, `Callable<BigDecimal>`, `Future.get()`. | `TransactionQueue.java`, `TransactionProducer.java`, `TransactionConsumer.java`, `SettlementCalculatorTask.java`. |
| **5. JDBC & Relational Database** | **8** | `DBConnection`, DAOs, `PreparedStatement`, `ResultSet`, atomic transactions with `commit` and `rollback`. | `CustomerDAO.java`, `AccountDAO.java`, `TransactionDAO.java`, `TransferService` with verified simulated failure rollback (`T14`). |
| **6. Lambda & Stream API Report** | **6** | `filter`, `map`, `sorted`, `distinct`, `groupingBy`, `counting`, `averagingDouble`, `summarizingDouble`. | `ReportService.java` providing multi-level asset breakdown, status groupings, and top high-value accounts. |
| **7. Generics & File Handling** | **4** | Generic `Repository<T>`, CSV import with `BufferedReader` tracking malformed rows, CSV export with `BufferedWriter`. | `Repository.java`, `CsvImporter.java` (line number error logger), `CsvExporter.java`, `data/customers.csv`. |
| **8. Integration, Testing & Viva** | **5** | Interactive CLI menu (options 1–13), reproducible demo script, automated test runner (18/18 Pass). | `VertexBankingApp.java`, `VertexTestRunner.java` (100% Pass rate), `sql/vertex_banking_schema.sql`. |

---

## 4. Database Schema & Relational Design

```
+--------------------+          +-----------------------------------+
|     customers      |          |             accounts              |
+--------------------+          +-----------------------------------+
| customer_id (PK)   |<---+     | account_number (PK)               |<---+
| name               |    |     | customer_id (FK -> customers)     |    |
| email (UNIQUE)     |    +-----| account_type                      |    |
| phone              |          | balance (CHECK >= 0)              |    |
| dob                |          | status (ACTIVE/FROZEN/CLOSED)     |    |
| created_at         |          | daily_limit                       |    |
+--------------------+          | daily_withdrawn_today             |    |
                                | last_withdrawal_date              |    |
                                +-----------------------------------+    |
                                                  |                      |
                                                  +----------------------+
                                                  |
                                                  v
                                +-----------------------------------+
                                |           transactions            |
                                +-----------------------------------+
                                | txn_id (PK)                       |
                                | account_number (FK -> accounts)   |
                                | type (DEPOSIT/WITHDRAW/TRANSFER)  |
                                | amount (CHECK > 0)                |
                                | txn_date                          |
                                | status (SUCCESS/FAILED/REJECTED)  |
                                | remarks                           |
                                +-----------------------------------+
```

---

## 5. Official Demonstration Dataset & Verification Results

### Standard Demonstration Sequence
1. **Register Customers:** `C101` (Ashu Sharma, 2000-05-10) and `C102` (Rohit Verma, 1999-08-22).
2. **Open Accounts:**
   - `A501` for `C101`: SAVINGS account with ₹50,000.00.
   - `A502` for `C102`: CURRENT account with ₹5,000.00.
3. **Atomic Fund Transfer:** Transfer ₹10,000.00 from `A501` to `A502`.
   - **Result:** `A501` = ₹40,000.00; `A502` = ₹15,000.00.
4. **Boundary Overdraft Test:** Attempt ₹60,000.00 withdrawal from `A502`.
   - **Result:** Rejected with `Insufficient funds. Balance: ₹15,000.00, Requested: ₹60,000.00`.
5. **Stream API Analytics:** Generates asset totals (₹55,000.00), counts grouped by status, and averages.

### Automated Test Matrix (T01 - T18)
All 18 standardized student tests pass automatically in `test.VertexTestRunner`:

| Test ID | Test Description | Expected Result | Actual Result | Status |
|:---:|:---|:---|:---|:---:|
| **T01** | Register valid customer | Customer saved and retrievable | Saved: Alice Smith | **PASS** |
| **T02** | Prevent duplicate customer ID | `DuplicateRecordException` thrown | Rejected duplicate ID | **PASS** |
| **T03** | Reject malformed email | `InvalidCustomerException` thrown | Rejected: Email format is invalid | **PASS** |
| **T04** | Reject future date of birth | `InvalidCustomerException` thrown | Rejected: Date of birth cannot be in future | **PASS** |
| **T05** | Reject SAVINGS deposit below ₹1,000 | `InvalidAccountException` thrown | Rejected: Below minimum required | **PASS** |
| **T06** | Accept SAVINGS deposit at boundary (₹1,000) | Account opened successfully | Balance: ₹1,000.00 | **PASS** |
| **T07** | Reject withdrawal exceeding balance | `InsufficientFundsException` thrown | Rejected: Insufficient funds | **PASS** |
| **T08** | Reject negative deposit | `InvalidAccountException` thrown | Rejected: Amount must be greater than zero | **PASS** |
| **T09** | Search for unknown customer ID | Returns empty `Optional` without crash | Present: false | **PASS** |
| **T10** | 5 concurrent threads withdraw ₹1,000 from ₹2,000 | Exactly 2 succeed, 3 rejected, balance ₹0.00 | Succeeded: 2, Failed: 3, Balance: ₹0.00 | **PASS** |
| **T11** | Producer-Consumer queue coordination | Tasks consumed without loss | Consumed: 4, Balance: ₹8,000.00 | **PASS** |
| **T12** | JDBC persistence across instances | Retrievable via independent DAO | Record retrieved intact | **PASS** |
| **T13** | SQL injection & apostrophe handling | PreparedStatement stores O'Brien safely | Loaded: Shaun O'Brien | **PASS** |
| **T14** | Atomic Transfer Rollback on mid-flight failure | Both balances restored to original | From: ₹10,000.00, To: ₹5,000.00 | **PASS** |
| **T15** | Stream API Grouped Status Report | Grouped counts match accounts | Grouped counts accurate | **PASS** |
| **T16** | CSV Import with malformed row handling | Valid imported, invalid rejected with line error | Imported: 3, Rejected: 2 (Logged reasons) | **PASS** |
| **T17** | Export account records to CSV | File generated with headers & rows | CSV file validated on disk | **PASS** |
| **T18** | Block transfer to FROZEN account | Transfer blocked with descriptive reason | Blocked: Account is FROZEN | **PASS** |

**Scorecard:** `18 / 18 TESTS PASSED (100.0%)`

---

## 6. How to Compile and Run

### Prerequisites
- Java JDK 17, 21, or 26 installed and present in `PATH`.
- MySQL 8.0+ (Optional: the system automatically operates with full JDBC fidelity using the built-in embedded engine if MySQL is offline).

### Quick Launch on Windows
1. **Compile all sources:**
   ```cmd
   D:\gg\vertex_banking\compile.bat
   ```
2. **Run Interactive Application:**
   ```cmd
   D:\gg\vertex_banking\run.bat
   ```
3. **Run Automated Test Suite (T01 - T18):**
   ```cmd
   D:\gg\vertex_banking\run-tests.bat
   ```

### Manual Command-Line Execution
```bash
# Navigate to project directory
cd D:\gg\vertex_banking

# Compile classes to bin/
javac -d bin -sourcepath src src/main/VertexBankingApp.java src/test/VertexTestRunner.java

# Copy configuration
copy src\vertex_banking.properties bin\

# Run Interactive CLI
java -cp bin main.VertexBankingApp

# Run Automated Test Suite
java -cp bin test.VertexTestRunner
```

---



## 7. Final Submission Checklist

- [x] Full source code structured cleanly across packages (`model`, `exception`, `repository`, `database`, `dao`, `service`, `concurrency`, `util`, `test`, `main`).
- [x] Complete MySQL schema with primary keys, foreign keys, and constraints (`sql/vertex_banking_schema.sql`).
- [x] Pre-loaded official demonstration dataset (`C101`, `C102`, `A501`, `A502`).
- [x] Concurrency stress test verifying thread safety (5 threads vs ₹2,000).
- [x] Producer-Consumer bounded queue using `wait()` / `notifyAll()` and `ExecutorService` with `Callable` / `Future`.
- [x] Stream API analytics (`groupingBy`, `counting`, `averagingDouble`, `sorted`, `map`, `distinct`).
- [x] Generic `Repository<T>` and CSV importer/exporter with line-by-line rejection auditing.
- [x] Automated test runner executing tests T01 through T18 with 100% Pass rate.
- [x] Windows launch and compile scripts (`compile.bat`, `run.bat`, `run-tests.bat`).

