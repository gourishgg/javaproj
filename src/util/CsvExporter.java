package util;

import model.Account;
import model.Customer;
import model.Transaction;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;

/**
 * Validated CSV file exporter.
 * Demonstrates:
 * - Milestone 7: BufferedWriter and FileWriter character streams
 * - try-with-resources for clean file flush and closure
 */
public class CsvExporter {

    public static void exportAccountsReport(File destination, List<Account> accounts, List<Customer> customers) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
            writer.write("account_number,customer_id,account_type,balance,status,daily_limit\n");
            for (Account a : accounts) {
                writer.write(String.format("%s,%s,%s,%.2f,%s,%.2f\n",
                        escapeCsv(a.getAccountNumber()),
                        escapeCsv(a.getCustomerId()),
                        a.getAccountType().name(),
                        a.getBalance(),
                        a.getStatus().name(),
                        a.getDailyLimit()));
            }
        }
    }

    public static void exportTransactionsReport(File destination, List<Transaction> transactions) throws IOException {
        File parent = destination.getParentFile();
        if (parent != null && !parent.exists()) {
            parent.mkdirs();
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(destination))) {
            writer.write("txn_id,account_number,type,amount,timestamp,status,remarks\n");
            for (Transaction t : transactions) {
                writer.write(String.format("%s,%s,%s,%.2f,%s,%s,%s\n",
                        escapeCsv(t.getTxnId()),
                        escapeCsv(t.getAccountNumber()),
                        t.getType().name(),
                        t.getAmount(),
                        t.getFormattedTimestamp(),
                        t.getStatus().name(),
                        escapeCsv(t.getRemarks())));
            }
        }
    }

    private static String escapeCsv(String input) {
        if (input == null) return "";
        if (input.contains(",") || input.contains("\"") || input.contains("\n")) {
            return "\"" + input.replace("\"", "\"\"") + "\"";
        }
        return input;
    }
}
