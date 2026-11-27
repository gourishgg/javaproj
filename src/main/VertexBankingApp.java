package main;

import concurrency.*;
import dao.*;
import exception.*;
import model.*;
import repository.*;
import service.*;
import test.VertexTestRunner;
import util.*;

import java.io.File;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.*;

/**
 * Master Console Application for Vertex Banking.
 *
 * Implements interactive menus, user workflows, error handling,
 * and seamless integration across all 8 development milestones.
 */
public class VertexBankingApp {
    private static final Scanner scanner = new Scanner(System.in);

    private final CustomerService customerService;
    private final AccountService accountService;
    private final TransferService transferService;
    private final ReportService reportService;
    private final BalanceManager balanceManager;

    public VertexBankingApp() {
        CustomerDAO customerDAO = new CustomerDAO();
        AccountDAO accountDAO = new AccountDAO();
        TransactionDAO transactionDAO = new TransactionDAO();

        Repository<Customer> customerRepo = new InMemoryRepository<>("Customer", Customer::getCustomerId);
        Repository<Account> accountRepo = new InMemoryRepository<>("Account", Account::getAccountNumber);
        Repository<Transaction> transactionRepo = new InMemoryRepository<>("Transaction", Transaction::getTxnId);

        this.customerService = new CustomerService(customerDAO, customerRepo);
        this.accountService = new AccountService(accountDAO, transactionDAO, accountRepo, transactionRepo, customerService);
        this.transferService = new TransferService(accountDAO, transactionDAO, accountRepo, transactionRepo, accountService);
        this.reportService = new ReportService(accountService);
        this.balanceManager = new BalanceManager();

        initializeDemoDataset();
    }

    private void initializeDemoDataset() {
        try {
            // Step 1: Register Asha (C101) & Rohan (C102)
            if (customerService.findCustomerById("C101").isEmpty()) {
                customerService.registerCustomer("C101", "Asha Sharma", "asha@vertex.com", "9876543210", LocalDate.of(2000, 5, 10));
            }
            if (customerService.findCustomerById("C102").isEmpty()) {
                customerService.registerCustomer("C102", "Rohan Verma", "rohan@vertex.com", "9876543211", LocalDate.of(1999, 8, 22));
            }

            // Step 2: Open A501 (Savings, ₹50,000) & A502 (Current, ₹5,000)
            if (accountService.getAccount("A501").isEmpty()) {
                accountService.openAccount("A501", "C101", AccountType.SAVINGS, new BigDecimal("50000.00"));
            }
            if (accountService.getAccount("A502").isEmpty()) {
                accountService.openAccount("A502", "C102", AccountType.CURRENT, new BigDecimal("5000.00"));
            }
        } catch (Exception ignored) {
            // Already initialized
        }
    }

    public void start() {
        printBanner();
        boolean running = true;
        while (running) {
            printMainMenu();
            int choice = readIntChoice();
            switch (choice) {
                case 1:  handleCustomerManagement(); break;
                case 2:  handleAccountManagement(); break;
                case 3:  handleDeposit(); break;
                case 4:  handleWithdraw(); break;
                case 5:  handleTransfer(); break;
                case 6:  handleTransactionHistory(); break;
                case 7:  handleAccountStatusUpdate(); break;
                case 8:  handleReports(); break;
                case 9:  handleCsvImport(); break;
                case 10: handleCsvExport(); break;
                case 11: handleProcessQueuedTransactions(); break;
                case 12: handleConcurrencyStressTest(); break;
                case 13: handleRunTestSuite(); break;
                case 0:
                    System.out.println("\nThank you for choosing Vertex Banking. Exiting application. Goodbye!\n");
                    running = false;
                    break;
                default:
                    System.out.println("\n[ERROR] Invalid option selected. Please choose a number between 0 and 13.");
            }
        }
    }

    private void printBanner() {
        System.out.println("================================================================================");
        System.out.println("  __      __       _             ____              _    _             ");
        System.out.println("  \\ \\    / /      | |           |  _ \\            | |  (_)            ");
        System.out.println("   \\ \\  / /__ _ __| |_ _____  __| |_) | __ _ _ __ | | ___ _ __   __ _ ");
        System.out.println("    \\ \\/ / _ \\ '__| __/ _ \\ \\/ /|  _ < / _` | '_ \\| |/ / | '_ \\ / _` |");
        System.out.println("     \\  /  __/ |  | ||  __/>  < | |_) | (_| | | | |   <| | | | | (_| |");
        System.out.println("      \\/ \\___|_|   \\__\\___/_/\\_\\|____/ \\__,_|_| |_|_|\\_\\_|_| |_|\\__, |");
        System.out.println("                                                                 __/ |");
        System.out.println("                                                                |___/ ");
        System.out.println("        Vertex Bank - Smart Banking, Simplified | Group G2 (Team 17)   ");
        System.out.println("        Database: " + database.DBConnection.getConnectionStatus());
        System.out.println("================================================================================");
    }

    private void printMainMenu() {
        System.out.println("\n====================== VERTEX BANKING MAIN MENU ======================");
        System.out.println("  1. Customer Management (Register, Search, Update, List)");
        System.out.println("  2. Account Management (Open Savings/Current, List by Customer)");
        System.out.println("  3. Deposit Funds");
        System.out.println("  4. Withdraw Funds");
        System.out.println("  5. Transfer Funds (Atomic transfer with rollback)");
        System.out.println("  6. Transaction History");
        System.out.println("  7. Account Status Update (ACTIVE / FROZEN / CLOSED)");
        System.out.println("  8. Analytics & Reports (Stream API: Grouping, Averages, Top Balances)");
        System.out.println("  9. Import Customers from CSV");
        System.out.println(" 10. Export Reports to CSV");
        System.out.println(" 11. Process Queued Transactions (Producer-Consumer & Executor)");
        System.out.println(" 12. Concurrency Stress Test Demo (Milestone 3: 5 Threads vs ₹2,000)");
        System.out.println(" 13. Run Automated System Test Suite (Tests T01 - T18)");
        System.out.println("  0. Exit");
        System.out.println("======================================================================");
        System.out.print("Enter your choice: ");
    }

    // ----------------------------------------------------
    // Option 1: Customer Management
    // ----------------------------------------------------
    private void handleCustomerManagement() {
        System.out.println("\n--- CUSTOMER MANAGEMENT ---");
        System.out.println("1. Register New Customer");
        System.out.println("2. Search Customer by ID");
        System.out.println("3. Update Customer Details");
        System.out.println("4. List All Registered Customers");
        System.out.println("0. Back to Main Menu");
        System.out.print("Select choice: ");
        int opt = readIntChoice();

        switch (opt) {
            case 1:
                try {
                    System.out.print("Enter Customer ID (e.g., C103): ");
                    String id = scanner.nextLine().trim();
                    System.out.print("Enter Full Name: ");
                    String name = scanner.nextLine().trim();
                    System.out.print("Enter Email Address: ");
                    String email = scanner.nextLine().trim();
                    System.out.print("Enter 10-digit Phone: ");
                    String phone = scanner.nextLine().trim();
                    System.out.print("Enter Date of Birth (YYYY-MM-DD): ");
                    String dobStr = scanner.nextLine().trim();
                    LocalDate dob = InputValidator.parseDate(dobStr);

                    Customer c = customerService.registerCustomer(id, name, email, phone, dob);
                    System.out.println("\n[SUCCESS] Customer registered successfully!");
                    System.out.println(c);
                } catch (DateTimeParseException e) {
                    System.out.println("\n[ERROR] Invalid date format. Please use YYYY-MM-DD.");
                } catch (VertexException e) {
                    System.out.println("\n[ERROR] Registration rejected: " + e.getMessage());
                }
                break;
            case 2:
                System.out.print("Enter Customer ID to search: ");
                String searchId = scanner.nextLine().trim();
                Optional<Customer> found = customerService.findCustomerById(searchId);
                if (found.isPresent()) {
                    System.out.println("\n[FOUND] " + found.get());
                    List<Account> accounts = accountService.getAccountsByCustomer(searchId);
                    System.out.println("Associated Accounts (" + accounts.size() + "):");
                    for (Account a : accounts) {
                        System.out.printf("  -> Acc: %s | Type: %s | Balance: ₹%.2f | Status: %s%n",
                                a.getAccountNumber(), a.getAccountType(), a.getBalance(), a.getStatus());
                    }
                } else {
                    System.out.println("\n[NOT FOUND] No customer found with ID: " + searchId);
                }
                break;
            case 3:
                try {
                    System.out.print("Enter Customer ID to update: ");
                    String upId = scanner.nextLine().trim();
                    Optional<Customer> existing = customerService.findCustomerById(upId);
                    if (existing.isEmpty()) {
                        System.out.println("\n[NOT FOUND] Customer not found.");
                        return;
                    }
                    System.out.print("Enter New Name (leave blank to keep '" + existing.get().getName() + "'): ");
                    String nName = scanner.nextLine().trim();
                    if (nName.isEmpty()) nName = existing.get().getName();

                    System.out.print("Enter New Email (leave blank to keep '" + existing.get().getEmail() + "'): ");
                    String nEmail = scanner.nextLine().trim();
                    if (nEmail.isEmpty()) nEmail = existing.get().getEmail();

                    System.out.print("Enter New Phone (leave blank to keep '" + existing.get().getPhone() + "'): ");
                    String nPhone = scanner.nextLine().trim();
                    if (nPhone.isEmpty()) nPhone = existing.get().getPhone();

                    boolean updated = customerService.updateCustomer(upId, nName, nEmail, nPhone, existing.get().getDob());
                    if (updated) {
                        System.out.println("\n[SUCCESS] Customer details updated successfully!");
                    } else {
                        System.out.println("\n[NOTICE] No changes were applied.");
                    }
                } catch (VertexException e) {
                    System.out.println("\n[ERROR] Update failed: " + e.getMessage());
                }
                break;
            case 4:
                List<Customer> list = customerService.getAllCustomers();
                System.out.println("\n--- ALL REGISTERED CUSTOMERS (" + list.size() + ") ---");
                if (list.isEmpty()) {
                    System.out.println("(No customers found)");
                } else {
                    for (Customer c : list) {
                        System.out.printf("  [%-6s] %-20s | %-25s | Ph: %-10s | Age: %d%n",
                                c.getCustomerId(), c.getName(), c.getEmail(), c.getPhone(), c.getAge());
                    }
                }
                break;
            case 0:
                break;
            default:
                System.out.println("[ERROR] Invalid choice.");
        }
    }

    // ----------------------------------------------------
    // Option 2: Account Management
    // ----------------------------------------------------
    private void handleAccountManagement() {
        System.out.println("\n--- ACCOUNT MANAGEMENT ---");
        System.out.println("1. Open New Account");
        System.out.println("2. View Account Details");
        System.out.println("3. List Accounts by Customer ID");
        System.out.println("4. List All Accounts");
        System.out.println("0. Back to Main Menu");
        System.out.print("Select choice: ");
        int opt = readIntChoice();

        switch (opt) {
            case 1:
                try {
                    System.out.print("Enter Account Number (e.g., A503): ");
                    String accNum = scanner.nextLine().trim();
                    System.out.print("Enter Customer ID: ");
                    String custId = scanner.nextLine().trim();

                    System.out.println("Select Account Type:");
                    System.out.println("  1. SAVINGS (Min opening deposit: ₹1,000.00)");
                    System.out.println("  2. CURRENT (Min opening deposit: ₹5,000.00)");
                    System.out.print("Choice: ");
                    int typeChoice = readIntChoice();
                    AccountType type = (typeChoice == 2) ? AccountType.CURRENT : AccountType.SAVINGS;

                    System.out.printf("Enter Opening Deposit Amount (Min ₹%.2f): ", type.getMinOpeningDeposit());
                    BigDecimal deposit = readBigDecimal();

                    Account acc = accountService.openAccount(accNum, custId, type, deposit);
                    System.out.println("\n[SUCCESS] Account successfully opened!");
                    System.out.println(acc);
                } catch (VertexException e) {
                    System.out.println("\n[ERROR] Account creation rejected: " + e.getMessage());
                }
                break;
            case 2:
                System.out.print("Enter Account Number: ");
                String num = scanner.nextLine().trim();
                Optional<Account> acc = accountService.getAccount(num);
                if (acc.isPresent()) {
                    Account a = acc.get();
                    System.out.println("\n[FOUND ACCOUNT]");
                    System.out.println("  Account Number    : " + a.getAccountNumber());
                    System.out.println("  Owner Customer ID : " + a.getCustomerId());
                    System.out.println("  Account Type      : " + a.getAccountType());
                    System.out.printf("  Current Balance   : ₹%,.2f%n", a.getBalance());
                    System.out.println("  Status            : " + a.getStatus());
                    System.out.printf("  Daily Limit       : ₹%,.2f%n", a.getDailyLimit());
                    System.out.printf("  Withdrawn Today   : ₹%,.2f%n", a.getDailyWithdrawnToday());
                } else {
                    System.out.println("\n[NOT FOUND] Account '" + num + "' does not exist.");
                }
                break;
            case 3:
                System.out.print("Enter Customer ID: ");
                String cId = scanner.nextLine().trim();
                List<Account> accounts = accountService.getAccountsByCustomer(cId);
                System.out.printf("\n--- ACCOUNTS OWNED BY CUSTOMER %s (%d) ---%n", cId, accounts.size());
                if (accounts.isEmpty()) {
                    System.out.println("No accounts found for this customer.");
                } else {
                    for (Account a : accounts) {
                        System.out.printf("  -> Acc: %-8s | Type: %-8s | Balance: ₹%,10.2f | Status: %s%n",
                                a.getAccountNumber(), a.getAccountType(), a.getBalance(), a.getStatus());
                    }
                }
                break;
            case 4:
                List<Account> all = accountService.getAllAccounts();
                System.out.printf("\n--- ALL BANK ACCOUNTS (%d) ---%n", all.size());
                for (Account a : all) {
                    System.out.printf("  Acc: %-8s | CustID: %-6s | Type: %-8s | Balance: ₹%,10.2f | Status: %-8s%n",
                            a.getAccountNumber(), a.getCustomerId(), a.getAccountType(), a.getBalance(), a.getStatus());
                }
                break;
            case 0:
                break;
            default:
                System.out.println("[ERROR] Invalid choice.");
        }
    }

    // ----------------------------------------------------
    // Option 3: Deposit
    // ----------------------------------------------------
    private void handleDeposit() {
        System.out.println("\n--- DEPOSIT FUNDS ---");
        System.out.print("Enter Account Number: ");
        String accNum = scanner.nextLine().trim();
        System.out.print("Enter Amount to Deposit: ");
        BigDecimal amount = readBigDecimal();
        System.out.print("Enter Remarks/Notes (optional): ");
        String remarks = scanner.nextLine().trim();

        try {
            Transaction txn = accountService.deposit(accNum, amount, remarks.isEmpty() ? "Over-the-counter Deposit" : remarks);
            Account a = accountService.getAccount(accNum).orElseThrow();
            System.out.println("\n[RESULT: DEPOSIT SUCCESSFUL]");
            System.out.println(txn);
            System.out.printf("Updated Account Balance: ₹%,.2f%n", a.getBalance());
        } catch (VertexException e) {
            System.out.println("\n[RESULT: REJECTED]");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 4: Withdraw
    // ----------------------------------------------------
    private void handleWithdraw() {
        System.out.println("\n--- WITHDRAW FUNDS ---");
        System.out.print("Enter Account Number: ");
        String accNum = scanner.nextLine().trim();
        System.out.print("Enter Amount to Withdraw: ");
        BigDecimal amount = readBigDecimal();
        System.out.print("Enter Remarks/Notes (optional): ");
        String remarks = scanner.nextLine().trim();

        try {
            Transaction txn = accountService.withdraw(accNum, amount, remarks.isEmpty() ? "ATM/Counter Withdrawal" : remarks);
            Account a = accountService.getAccount(accNum).orElseThrow();
            System.out.println("\n[RESULT: WITHDRAWAL SUCCESSFUL]");
            System.out.println(txn);
            System.out.printf("Updated Account Balance: ₹%,.2f%n", a.getBalance());
        } catch (InsufficientFundsException e) {
            System.out.println("\n[RESULT: REJECTED]");
            System.out.printf("Reason: Insufficient funds. Balance: ₹%,.2f, Requested: ₹%,.2f%n",
                    e.getCurrentBalance(), e.getRequestedAmount());
        } catch (VertexException e) {
            System.out.println("\n[RESULT: REJECTED]");
            System.out.println("Reason: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 5: Transfer Funds
    // ----------------------------------------------------
    private void handleTransfer() {
        System.out.println("\n--- ATOMIC FUND TRANSFER ---");
        System.out.print("Enter From Account: ");
        String fromAcc = scanner.nextLine().trim();
        System.out.print("Enter To Account: ");
        String toAcc = scanner.nextLine().trim();
        System.out.print("Enter Amount: ");
        BigDecimal amount = readBigDecimal();

        System.out.print("Demonstrate simulated failure & rollback? (y/N): ");
        String sim = scanner.nextLine().trim();
        boolean simulateFailure = sim.equalsIgnoreCase("y");

        try {
            TransferService.TransferResult res = transferService.transferFundsInternal(fromAcc, toAcc, amount, simulateFailure);
            System.out.println("\n[RESULT: TRANSFER SUCCESSFUL]");
            System.out.printf("Debit Txn Ref : %s%n", res.getDebitTxnId());
            System.out.printf("Credit Txn Ref: %s%n", res.getCreditTxnId());
            System.out.printf("%s balance: ₹%,.2f%n", fromAcc.toUpperCase(), res.getFromBalance());
            System.out.printf("%s balance: ₹%,.2f%n", toAcc.toUpperCase(), res.getToBalance());
        } catch (VertexException e) {
            System.out.println("\n[RESULT: TRANSFER FAILED / REJECTED]");
            System.out.println("Reason: " + e.getMessage());
            accountService.getAccount(fromAcc).ifPresent(a ->
                    System.out.printf("Current %s balance: ₹%,.2f%n", fromAcc.toUpperCase(), a.getBalance()));
            accountService.getAccount(toAcc).ifPresent(a ->
                    System.out.printf("Current %s balance: ₹%,.2f%n", toAcc.toUpperCase(), a.getBalance()));
        }
    }

    // ----------------------------------------------------
    // Option 6: Transaction History
    // ----------------------------------------------------
    private void handleTransactionHistory() {
        System.out.println("\n--- TRANSACTION HISTORY ---");
        System.out.println("1. View Transactions for Specific Account");
        System.out.println("2. View All System Transactions");
        System.out.print("Select choice: ");
        int opt = readIntChoice();

        if (opt == 1) {
            System.out.print("Enter Account Number: ");
            String acc = scanner.nextLine().trim();
            List<Transaction> list = accountService.getTransactionsForAccount(acc);
            System.out.printf("\n--- AUDIT LOG FOR ACCOUNT %s (%d) ---%n", acc.toUpperCase(), list.size());
            if (list.isEmpty()) {
                System.out.println("No transaction records found.");
            } else {
                for (Transaction t : list) {
                    System.out.println(t);
                }
            }
        } else {
            List<Transaction> all = accountService.getAllTransactions();
            System.out.printf("\n--- ALL RECENT TRANSACTIONS (%d) ---%n", all.size());
            for (Transaction t : all) {
                System.out.println(t);
            }
        }
    }

    // ----------------------------------------------------
    // Option 7: Account Status Update
    // ----------------------------------------------------
    private void handleAccountStatusUpdate() {
        System.out.println("\n--- ACCOUNT STATUS LIFECYCLE UPDATE ---");
        System.out.print("Enter Account Number: ");
        String accNum = scanner.nextLine().trim();

        Optional<Account> accOpt = accountService.getAccount(accNum);
        if (accOpt.isEmpty()) {
            System.out.println("[NOT FOUND] Account not found.");
            return;
        }

        Account acc = accOpt.get();
        System.out.println("Current Status: " + acc.getStatus());
        System.out.println("Select New Status:");
        System.out.println("  1. ACTIVE (Unlock account for transactions)");
        System.out.println("  2. FROZEN (Temporarily block deposits & withdrawals)");
        System.out.println("  3. CLOSED (Permanently close; requires zero balance)");
        System.out.print("Choice: ");
        int choice = readIntChoice();

        AccountStatus newStatus = null;
        if (choice == 1) newStatus = AccountStatus.ACTIVE;
        else if (choice == 2) newStatus = AccountStatus.FROZEN;
        else if (choice == 3) newStatus = AccountStatus.CLOSED;

        if (newStatus == null) {
            System.out.println("[ERROR] Invalid status choice.");
            return;
        }

        try {
            accountService.updateStatus(accNum, newStatus);
            System.out.println("\n[SUCCESS] Account " + accNum.toUpperCase() + " status transitioned to: " + newStatus);
        } catch (VertexException e) {
            System.out.println("\n[ERROR] Status update rejected: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 8: Reports (Stream API)
    // ----------------------------------------------------
    private void handleReports() {
        System.out.println("\nGenerating analytical reports using Java Stream API...");
        String report = reportService.generateSystemReport();
        System.out.println(report);
    }

    // ----------------------------------------------------
    // Option 9: CSV Import
    // ----------------------------------------------------
    private void handleCsvImport() {
        System.out.println("\n--- BULK IMPORT CUSTOMERS FROM CSV ---");
        System.out.print("Enter CSV path (press Enter for default 'data/customers.csv'): ");
        String path = scanner.nextLine().trim();
        if (path.isEmpty()) path = "data/customers.csv";

        File file = new File(path);
        try {
            CsvImporter.ImportSummary summary = CsvImporter.importCustomers(file, customerService);
            System.out.println("\n========================================================");
            System.out.println("                 CSV INGESTION SUMMARY                  ");
            System.out.println("========================================================");
            System.out.printf("Total Rows Processed : %d%n", summary.getTotalRows());
            System.out.printf("Successfully Imported: %d%n", summary.getImportedCount());
            System.out.printf("Rejected / Errors    : %d%n", summary.getRejectedCount());
            System.out.println("--------------------------------------------------------");

            if (!summary.getErrors().isEmpty()) {
                System.out.println("DETAILED ERROR AUDIT LOG:");
                for (CsvImporter.RowError err : summary.getErrors()) {
                    System.out.println("  * " + err);
                }
            }
            System.out.println("========================================================");
        } catch (Exception e) {
            System.out.println("\n[ERROR] CSV import failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 10: CSV Export
    // ----------------------------------------------------
    private void handleCsvExport() {
        System.out.println("\n--- EXPORT RECORDS TO CSV ---");
        System.out.println("1. Export Account Summary Report");
        System.out.println("2. Export Transaction History Report");
        System.out.print("Choice: ");
        int choice = readIntChoice();

        try {
            if (choice == 1) {
                File out = new File("data/exported_accounts.csv");
                CsvExporter.exportAccountsReport(out, accountService.getAllAccounts(), customerService.getAllCustomers());
                System.out.println("\n[SUCCESS] Accounts exported successfully to: " + out.getAbsolutePath());
            } else {
                File out = new File("data/exported_transactions.csv");
                CsvExporter.exportTransactionsReport(out, accountService.getAllTransactions());
                System.out.println("\n[SUCCESS] Transactions exported successfully to: " + out.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("\n[ERROR] Export failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 11: Producer-Consumer & Executor Processing
    // ----------------------------------------------------
    private void handleProcessQueuedTransactions() {
        System.out.println("\n================================================================================");
        System.out.println("       MILESTONE 4: PRODUCER-CONSUMER & EXECUTOR WORKER SIMULATION             ");
        System.out.println("================================================================================");

        Account demoAcc = accountService.getAccount("A501").orElse(null);
        if (demoAcc == null) {
            System.out.println("[ERROR] Account A501 not found for worker simulation.");
            return;
        }

        TransactionQueue queue = new TransactionQueue(3); // Small capacity to demonstrate queue full/wait
        List<TransactionTask> tasks = new ArrayList<>();
        tasks.add(new TransactionTask("Q-Txn-1", demoAcc, new BigDecimal("100.00"), balanceManager));
        tasks.add(new TransactionTask("Q-Txn-2", demoAcc, new BigDecimal("200.00"), balanceManager));
        tasks.add(new TransactionTask("Q-Txn-3", demoAcc, new BigDecimal("150.00"), balanceManager));
        tasks.add(new TransactionTask("Q-Txn-4", demoAcc, new BigDecimal("250.00"), balanceManager));
        tasks.add(new TransactionTask("Q-Txn-5", demoAcc, new BigDecimal("300.00"), balanceManager));

        TransactionProducer producer = new TransactionProducer("Ingestion-Worker", queue, tasks);
        TransactionConsumer consumer = new TransactionConsumer("Settlement-Worker", queue);

        Thread pThread = new Thread(producer);
        Thread cThread = new Thread(consumer);

        cThread.start();
        pThread.start();

        try {
            pThread.join();
            Thread.sleep(200);
            queue.stop();
            cThread.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        System.out.println("\n>> Step 2: Running Asynchronous Callable Batch Aggregator via ExecutorService...");
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Callable<BigDecimal> batchTask = new SettlementCalculatorTask("Daily-Close-Batch", accountService.getAllAccounts());
            Future<BigDecimal> future = executor.submit(batchTask);

            System.out.println("   [MAIN THREAD] Waiting for Future.get() completion...");
            BigDecimal totalSettled = future.get();
            System.out.printf(">> [FUTURE RESULT] Total balance aggregated across portfolio: ₹%,.2f%n", totalSettled);

            executor.shutdown();
            if (executor.awaitTermination(3, TimeUnit.SECONDS)) {
                System.out.println(">> ExecutorService shut down cleanly.");
            }
        } catch (Exception e) {
            System.out.println("[ERROR] Executor error: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 12: Concurrency Stress Test Demo (Milestone 3)
    // ----------------------------------------------------
    private void handleConcurrencyStressTest() {
        System.out.println("\n================================================================================");
        System.out.println("          MILESTONE 3: MULTITHREADING & SYNCHRONIZATION STRESS TEST            ");
        System.out.println("   Testing 5 concurrent withdrawal tasks of ₹1,000 each against a ₹2,000 account ");
        System.out.println("================================================================================");

        try {
            String testAccNum = "STRESS-" + System.currentTimeMillis() % 10000;
            Account testAcc = accountService.openAccount(testAccNum, "C101", AccountType.SAVINGS, new BigDecimal("2000.00"));

            System.out.printf(">> Created temporary stress-test account %s with initial balance: ₹%,.2f%n",
                    testAccNum, testAcc.getBalance());
            System.out.println(">> Spawning 5 parallel threads targeting account simultaneously...\n");

            ExecutorService pool = Executors.newFixedThreadPool(5);
            List<TransactionTask> tasks = new ArrayList<>();
            for (int i = 1; i <= 5; i++) {
                tasks.add(new TransactionTask("WorkerThread-" + i, testAcc, new BigDecimal("1000.00"), balanceManager));
            }

            for (TransactionTask t : tasks) {
                pool.submit(t);
            }
            pool.shutdown();
            pool.awaitTermination(5, TimeUnit.SECONDS);

            long succeeded = tasks.stream().filter(TransactionTask::isSuccessful).count();
            long failed = tasks.stream().filter(t -> !t.isSuccessful()).count();

            System.out.println("\n--------------------------------------------------------------------------------");
            System.out.printf("CONCURRENCY TEST OUTCOME:%n");
            System.out.printf("  * Total Withdrawal Attempts : 5 (₹1,000 each)%n");
            System.out.printf("  * Successful Transactions   : %d (Expected: 2)%n", succeeded);
            System.out.printf("  * Rejected Transactions     : %d (Expected: 3)%n", failed);
            System.out.printf("  * Final Account Balance     : ₹%,.2f (Expected: ₹0.00, Never Negative)%n", testAcc.getBalance());

            if (succeeded == 2 && failed == 3 && testAcc.getBalance().compareTo(BigDecimal.ZERO) == 0) {
                System.out.println("\n>>> [VERIFICATION SUCCESS] Shared critical section successfully guarded by synchronized!");
            } else {
                System.out.println("\n>>> [VERIFICATION WARNING] Anomaly observed. Check thread lock monitors.");
            }
            System.out.println("--------------------------------------------------------------------------------");
        } catch (Exception e) {
            System.out.println("[ERROR] Concurrency demo failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------------
    // Option 13: Run Automated Test Suite
    // ----------------------------------------------------
    private void handleRunTestSuite() {
        VertexTestRunner.runAllTests();
    }

    // ----------------------------------------------------
    // Helper Input Methods
    // ----------------------------------------------------
    private int readIntChoice() {
        try {
            String line = scanner.nextLine().trim();
            return Integer.parseInt(line);
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private BigDecimal readBigDecimal() {
        while (true) {
            try {
                String line = scanner.nextLine().trim().replace(",", "");
                BigDecimal val = new BigDecimal(line);
                if (val.compareTo(BigDecimal.ZERO) <= 0) {
                    System.out.print("Amount must be positive. Re-enter: ");
                    continue;
                }
                return val;
            } catch (NumberFormatException e) {
                System.out.print("Invalid numeric format. Re-enter: ");
            }
        }
    }

    public static void main(String[] args) {
        VertexBankingApp app = new VertexBankingApp();
        app.start();
    }
}
