package test;

import concurrency.*;
import dao.*;
import exception.*;
import model.*;
import repository.*;
import service.*;
import util.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;

/**
 * Automated Verification Suite for Vertex Banking.
 * Executes all 18 test cases (T01 - T18) specified in Section 7 of the Project Handbook
 * and generates a Pass/Fail scorecard.
 */
public class VertexTestRunner {
    private static int totalTests = 0;
    private static int passedTests = 0;
    private static final List<TestRecord> testResults = new ArrayList<>();

    public static class TestRecord {
        String id;
        String name;
        String expected;
        String actual;
        boolean passed;

        public TestRecord(String id, String name, String expected, String actual, boolean passed) {
            this.id = id;
            this.name = name;
            this.expected = expected;
            this.actual = actual;
            this.passed = passed;
        }
    }

    private static void recordResult(String id, String name, String expected, String actual, boolean passed) {
        totalTests++;
        if (passed) passedTests++;
        testResults.add(new TestRecord(id, name, expected, actual, passed));
        System.out.printf("[%s] %-40s -> %s%n", id, name, passed ? "[PASS]" : "[FAIL]");
        if (!passed) {
            System.out.println("   Expected: " + expected);
            System.out.println("   Actual:   " + actual);
        }
    }

    public static void runAllTests() {
        totalTests = 0;
        passedTests = 0;
        testResults.clear();

        cleanupTestRecords();

        System.out.println("================================================================================");
        System.out.println("               VERTEX BANKING - AUTOMATED VERIFICATION SUITE                    ");
        System.out.println("                  Testing Requirements T01 through T18                         ");
        System.out.println("   Database: " + database.DBConnection.getConnectionStatus());
        System.out.println("================================================================================");

        CustomerDAO customerDAO = new CustomerDAO();
        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO transactionDAO = new TransactionDAO();

        Repository<Customer> customerRepo = new InMemoryRepository<>("Customer", Customer::getCustomerId);
        Repository<Account> accountRepo = new InMemoryRepository<>("Account", Account::getAccountNumber);
        Repository<Transaction> transactionRepo = new InMemoryRepository<>("Transaction", Transaction::getTxnId);

        CustomerService customerService = new CustomerService(customerDAO, customerRepo);
        AccountService accountService = new AccountService(accountDAO, transactionDAO, accountRepo, transactionRepo, customerService);
        TransferService transferService = new TransferService(accountDAO, transactionDAO, accountRepo, transactionRepo, accountService);
        ReportService reportService = new ReportService(accountService);

        // Pre-populate official demo baseline
        try {
            customerService.registerCustomer("C101", "Asha Sharma", "asha@vertex.com", "9876543210", LocalDate.of(2000, 5, 10));
            customerService.registerCustomer("C102", "Rohan Verma", "rohan@vertex.com", "9876543211", LocalDate.of(1999, 8, 22));
            accountService.openAccount("A501", "C101", AccountType.SAVINGS, new BigDecimal("50000.00"));
            accountService.openAccount("A502", "C102", AccountType.CURRENT, new BigDecimal("5000.00"));
        } catch (Exception ignored) {}

        // --- T01: Register a customer with valid details ---
        try {
            Customer c = customerService.registerCustomer("T_C01", "Alice Smith", "alice.smith@vertex.com", "9876543220", LocalDate.of(1995, 3, 15));
            recordResult("T01", "Register valid customer", "Customer saved & retrievable",
                    "Saved: " + c.getName(), c.getCustomerId().equals("T_C01"));
        } catch (Exception e) {
            recordResult("T01", "Register valid customer", "Customer saved", e.getMessage(), false);
        }

        // --- T02: Register an existing customer ID ---
        try {
            customerService.registerCustomer("T_C01", "Alice Duplicate", "alice2@vertex.com", "9876543221", LocalDate.of(1995, 3, 15));
            recordResult("T02", "Prevent duplicate customer ID", "DuplicateRecordException thrown", "Allowed duplicate registration", false);
        } catch (DuplicateRecordException e) {
            recordResult("T02", "Prevent duplicate customer ID", "DuplicateRecordException thrown", e.getMessage(), true);
        } catch (Exception e) {
            recordResult("T02", "Prevent duplicate customer ID", "DuplicateRecordException thrown", e.toString(), false);
        }

        // --- T03: Register with invalid email format ---
        try {
            customerService.registerCustomer("T_C03", "Bad Email", "asha-mail", "9876543222", LocalDate.of(1995, 3, 15));
            recordResult("T03", "Reject malformed email", "InvalidCustomerException thrown", "Allowed invalid email", false);
        } catch (InvalidCustomerException e) {
            boolean matches = e.getMessage().toLowerCase().contains("email format is invalid");
            recordResult("T03", "Reject malformed email", "Email format is invalid message", e.getMessage(), matches);
        } catch (Exception e) {
            recordResult("T03", "Reject malformed email", "InvalidCustomerException thrown", e.toString(), false);
        }

        // --- T04: Register with a future date of birth ---
        try {
            customerService.registerCustomer("T_C04", "Future User", "future@vertex.com", "9876543223", LocalDate.now().plusDays(5));
            recordResult("T04", "Reject future date of birth", "Date of birth cannot be in future", "Allowed future DOB", false);
        } catch (InvalidCustomerException e) {
            boolean matches = e.getMessage().toLowerCase().contains("future");
            recordResult("T04", "Reject future date of birth", "Date of birth cannot be in future", e.getMessage(), matches);
        } catch (Exception e) {
            recordResult("T04", "Reject future date of birth", "InvalidCustomerException thrown", e.toString(), false);
        }

        // --- T05: Open SAVINGS account with ₹500 (below ₹1,000 min) ---
        try {
            accountService.openAccount("T_A05", "C101", AccountType.SAVINGS, new BigDecimal("500.00"));
            recordResult("T05", "Reject SAVINGS deposit below ₹1,000", "InvalidAccountException thrown", "Allowed ₹500 opening deposit", false);
        } catch (InvalidAccountException e) {
            boolean matches = e.getMessage().contains("below the minimum");
            recordResult("T05", "Reject SAVINGS deposit below ₹1,000", "Rejected: Below minimum", e.getMessage(), matches);
        } catch (Exception e) {
            recordResult("T05", "Reject SAVINGS deposit below ₹1,000", "InvalidAccountException thrown", e.toString(), false);
        }

        // --- T06: Open SAVINGS account with exactly ₹1,000 (Boundary case) ---
        try {
            Account a = accountService.openAccount("T_A06", "C101", AccountType.SAVINGS, new BigDecimal("1000.00"));
            recordResult("T06", "Accept SAVINGS deposit at boundary (₹1,000)", "Account opened successfully",
                    "Balance: ₹" + a.getBalance(), a.getBalance().compareTo(new BigDecimal("1000.00")) == 0);
        } catch (Exception e) {
            recordResult("T06", "Accept SAVINGS deposit at boundary (₹1,000)", "Account opened", e.getMessage(), false);
        }

        // --- T07: Withdraw ₹60,000 from account holding ₹15,000 ---
        try {
            Account a7 = accountService.openAccount("T_A07", "C102", AccountType.CURRENT, new BigDecimal("15000.00"));
            accountService.withdraw("T_A07", new BigDecimal("60000.00"), "Overdraft attempt");
            recordResult("T07", "Reject withdrawal exceeding balance", "InsufficientFundsException thrown", "Overdraft allowed", false);
        } catch (InsufficientFundsException e) {
            recordResult("T07", "Reject withdrawal exceeding balance", "Insufficient funds message", e.getMessage(), true);
        } catch (Exception e) {
            recordResult("T07", "Reject withdrawal exceeding balance", "InsufficientFundsException thrown", e.toString(), false);
        }

        // --- T08: Deposit negative amount (-100) ---
        try {
            accountService.deposit("T_A06", new BigDecimal("-100.00"), "Negative test");
            recordResult("T08", "Reject negative deposit", "InvalidAccountException thrown", "Negative deposit accepted", false);
        } catch (InvalidAccountException e) {
            boolean matches = e.getMessage().toLowerCase().contains("greater than zero");
            recordResult("T08", "Reject negative deposit", "Amount must be greater than zero", e.getMessage(), matches);
        } catch (Exception e) {
            recordResult("T08", "Reject negative deposit", "InvalidAccountException", e.toString(), false);
        }

        // --- T09: Search for unknown customer ID ---
        Optional<Customer> unknown = customerService.findCustomerById("UNKNOWN_999");
        recordResult("T09", "Search for unknown customer ID", "Returns Optional.empty() without error",
                "Present: " + unknown.isPresent(), unknown.isEmpty());

        // --- T10: Five concurrent ₹1,000 withdrawals from a ₹2,000 account ---
        try {
            Account concAcc = accountService.openAccount("T_A10_CONC", "C101", AccountType.SAVINGS, new BigDecimal("2000.00"));
            BalanceManager bm = new BalanceManager();
            ExecutorService concExec = Executors.newFixedThreadPool(5);
            List<TransactionTask> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                tasks.add(new TransactionTask("Task-" + i, concAcc, new BigDecimal("1000.00"), bm));
            }

            for (TransactionTask t : tasks) {
                concExec.submit(t);
            }
            concExec.shutdown();
            concExec.awaitTermination(5, TimeUnit.SECONDS);

            long successCount = tasks.stream().filter(TransactionTask::isSuccessful).count();
            long failCount = tasks.stream().filter(t -> !t.isSuccessful()).count();
            BigDecimal endBalance = concAcc.getBalance();

            boolean pass = (successCount == 2) && (failCount == 3) && (endBalance.compareTo(BigDecimal.ZERO) == 0);
            recordResult("T10", "Concurrency: 5 threads withdraw ₹1,000 from ₹2,000",
                    "Exactly 2 succeed, 3 rejected, final balance ₹0.00",
                    String.format("Succeeded: %d, Failed: %d, Final Balance: ₹%.2f", successCount, failCount, endBalance),
                    pass);
        } catch (Exception e) {
            recordResult("T10", "Concurrency: 5 threads withdraw ₹1,000 from ₹2,000", "2 succeed, balance 0", e.getMessage(), false);
        }

        // --- T11: Add and consume queued transaction tasks ---
        try {
            Account qAcc = accountService.openAccount("T_A11_QUEUE", "C101", AccountType.SAVINGS, new BigDecimal("10000.00"));
            TransactionQueue queue = new TransactionQueue(5);
            BalanceManager bm = new BalanceManager();
            List<TransactionTask> qTasks = new ArrayList<>();
            for (int i = 1; i <= 4; i++) {
                qTasks.add(new TransactionTask("QTask-" + i, qAcc, new BigDecimal("500.00"), bm));
            }

            TransactionProducer producer = new TransactionProducer("Worker-Producer", queue, qTasks);
            TransactionConsumer consumer = new TransactionConsumer("Worker-Consumer", queue);

            Thread pThread = new Thread(producer);
            Thread cThread = new Thread(consumer);
            cThread.start();
            pThread.start();

            pThread.join(2000);
            Thread.sleep(300);
            queue.stop();
            cThread.join(2000);

            boolean queuePass = (consumer.getProcessedCount() == 4) &&
                    (qAcc.getBalance().compareTo(new BigDecimal("8000.00")) == 0);
            recordResult("T11", "Producer-Consumer Queue Processing", "All 4 tasks consumed without loss",
                    "Consumed: " + consumer.getProcessedCount() + ", Balance: ₹" + qAcc.getBalance(), queuePass);
        } catch (Exception e) {
            recordResult("T11", "Producer-Consumer Queue Processing", "All tasks consumed", e.getMessage(), false);
        }

        // --- T12: PreparedStatement / JDBC persistence across instances ---
        try {
            Customer c12 = customerService.registerCustomer("T_C12", "Persistence User", "persist@vertex.com", "9876543299", LocalDate.of(1992, 1, 1));
            CustomerDAO freshDAO = new CustomerDAO();
            Optional<Customer> found = freshDAO.findById("T_C12");
            recordResult("T12", "JDBC record persistence across instances", "Retrievable from fresh DAO",
                    "Found: " + found.map(Customer::getName).orElse("NONE"), found.isPresent());
        } catch (Exception e) {
            recordResult("T12", "JDBC record persistence", "Retrievable", e.getMessage(), false);
        }

        // --- T13: Name with apostrophe (e.g. O'Brien) handled safely by PreparedStatement ---
        try {
            Customer c13 = customerService.registerCustomer("T_C13", "Shaun O'Brien", "obrien@vertex.com", "9876543298", LocalDate.of(1990, 7, 20));
            Optional<Customer> reloaded = customerService.findCustomerById("T_C13");
            boolean match = reloaded.isPresent() && reloaded.get().getName().equals("Shaun O'Brien");
            recordResult("T13", "PreparedStatement SQL injection & apostrophe handling", "Stores O'Brien as pure data",
                    "Loaded: " + reloaded.map(Customer::getName).orElse("NONE"), match);
        } catch (Exception e) {
            recordResult("T13", "PreparedStatement apostrophe handling", "Safe storage", e.getMessage(), false);
        }

        // --- T14: Force failure after debit step in transfer (Rollback verification) ---
        try {
            Account from14 = accountService.openAccount("T_A14_FROM", "C101", AccountType.SAVINGS, new BigDecimal("10000.00"));
            Account to14 = accountService.openAccount("T_A14_TO", "C102", AccountType.SAVINGS, new BigDecimal("5000.00"));

            try {
                transferService.transferFundsInternal("T_A14_FROM", "T_A14_TO", new BigDecimal("3000.00"), true);
            } catch (TransactionException ex) {
                // Expected simulated failure
            }

            BigDecimal fromBal = from14.getBalance();
            BigDecimal toBal = to14.getBalance();
            boolean rollbackSuccess = (fromBal.compareTo(new BigDecimal("10000.00")) == 0) &&
                                      (toBal.compareTo(new BigDecimal("5000.00")) == 0);
            recordResult("T14", "Atomic Transfer Rollback on mid-flight failure",
                    "From: ₹10,000.00, To: ₹5,000.00 (Unchanged)",
                    String.format("From: ₹%.2f, To: ₹%.2f", fromBal, toBal), rollbackSuccess);
        } catch (Exception e) {
            recordResult("T14", "Atomic Transfer Rollback", "Balances restored", e.getMessage(), false);
        }

        // --- T15: Generate grouped status report (Stream API) ---
        try {
            Map<AccountStatus, Long> counts = reportService.getAccountCountByStatus();
            boolean reportValid = counts.containsKey(AccountStatus.ACTIVE) && counts.get(AccountStatus.ACTIVE) > 0;
            recordResult("T15", "Stream API Grouped Status Report", "Counts match account records",
                    "Active count: " + counts.get(AccountStatus.ACTIVE), reportValid);
        } catch (Exception e) {
            recordResult("T15", "Stream API Grouped Status Report", "Valid grouping", e.getMessage(), false);
        }

        // --- T16: CSV Import with malformed row handling ---
        try {
            File sampleCsv = new File("data/customers.csv");
            if (!sampleCsv.exists()) {
                sampleCsv = new File("D:/javaroj/data/customers.csv");
            }
            if (!sampleCsv.exists()) {
                sampleCsv = new File("D:/gg/vertex_banking/data/customers.csv");
            }
            CsvImporter.ImportSummary summary = CsvImporter.importCustomers(sampleCsv, customerService);
            boolean csvPass = (summary.getImportedCount() >= 1) && (summary.getRejectedCount() >= 1);
            recordResult("T16", "CSV Import with malformed row handling",
                    "Valid rows imported, invalid rows rejected with line reason",
                    String.format("Imported: %d, Rejected: %d (e.g. %s)",
                            summary.getImportedCount(), summary.getRejectedCount(),
                            summary.getErrors().isEmpty() ? "None" : summary.getErrors().get(0).getReason()),
                    csvPass);
        } catch (Exception e) {
            recordResult("T16", "CSV Import", "Valid import & reject tracking", e.getMessage(), false);
        }

        // --- T17: Export report to CSV file ---
        try {
            File exportFile = new File("data/test_exported_accounts.csv");
            CsvExporter.exportAccountsReport(exportFile, accountService.getAllAccounts(), customerService.getAllCustomers());
            boolean exported = exportFile.exists() && exportFile.length() > 0;
            recordResult("T17", "Export account records to CSV", "File created with headers & rows",
                    "File exists: " + exported + ", Size: " + exportFile.length() + " bytes", exported);
        } catch (Exception e) {
            recordResult("T17", "Export account records to CSV", "File created", e.getMessage(), false);
        }

        // --- T18: Transfer to FROZEN account rejected ---
        try {
            Account frozenAcc = accountService.openAccount("T_A18_FROZEN", "C102", AccountType.CURRENT, new BigDecimal("6000.00"));
            accountService.updateStatus("T_A18_FROZEN", AccountStatus.FROZEN);
            transferService.transferFunds("A501", "T_A18_FROZEN", new BigDecimal("1000.00"));
            recordResult("T18", "Block transfer to FROZEN account", "InvalidAccountException thrown", "Transfer accepted to frozen acc", false);
        } catch (InvalidAccountException e) {
            boolean blocked = e.getMessage().contains("FROZEN");
            recordResult("T18", "Block transfer to FROZEN account", "Transfer blocked with reason", e.getMessage(), blocked);
        } catch (Exception e) {
            recordResult("T18", "Block transfer to FROZEN account", "InvalidAccountException", e.toString(), false);
        }

        // Print final summary
        System.out.println("================================================================================");
        System.out.printf("   TEST EXECUTION COMPLETED: %d / %d TESTS PASSED (%.1f%%)%n",
                passedTests, totalTests, (passedTests * 100.0 / totalTests));
        System.out.println("================================================================================");

        cleanupTestRecords();
    }

    private static void cleanupTestRecords() {
        try (java.sql.Connection conn = database.DBConnection.getConnection()) {
            if (database.DBConnection.isUsingSimulated()) {
                return;
            }
            try (java.sql.Statement st = conn.createStatement()) {
                st.executeUpdate("DELETE FROM transactions WHERE account_number LIKE 'T\\_%'");
                st.executeUpdate("DELETE FROM accounts WHERE account_number LIKE 'T\\_%'");
                st.executeUpdate("DELETE FROM customers WHERE customer_id LIKE 'T\\_%'");
                st.executeUpdate("DELETE FROM customers WHERE customer_id IN ('C201', 'C202', 'C203') " +
                        "AND customer_id NOT IN (SELECT DISTINCT customer_id FROM accounts)");
            }
        } catch (Exception ignored) {
        }
    }

    public static void main(String[] args) {
        runAllTests();
    }
}
