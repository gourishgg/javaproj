package util;

import exception.VertexException;
import model.Customer;
import service.CustomerService;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

/**
 * Validated CSV file importer.
 * Demonstrates:
 * - Milestone 7: Character streams (BufferedReader, FileReader)
 * - try-with-resources for deterministic file handle closing
 * - Row-by-row structural & semantic validation
 * - Error tracking with row numbers and exact rejection reasons
 */
public class CsvImporter {

    public static class RowError {
        private final int lineNumber;
        private final String rawContent;
        private final String reason;

        public RowError(int lineNumber, String rawContent, String reason) {
            this.lineNumber = lineNumber;
            this.rawContent = rawContent;
            this.reason = reason;
        }

        public int getLineNumber() { return lineNumber; }
        public String getRawContent() { return rawContent; }
        public String getReason() { return reason; }

        @Override
        public String toString() {
            return String.format("Line %d: [REJECTED] Reason: %s (Raw: '%s')", lineNumber, reason, rawContent);
        }
    }

    public static class ImportSummary {
        private int totalRows = 0;
        private int importedCount = 0;
        private final List<RowError> errors = new ArrayList<>();
        private final List<Customer> importedCustomers = new ArrayList<>();

        public void recordSuccess(Customer customer) {
            totalRows++;
            importedCount++;
            importedCustomers.add(customer);
        }

        public void recordError(int lineNumber, String rawContent, String reason) {
            totalRows++;
            errors.add(new RowError(lineNumber, rawContent, reason));
        }

        public int getTotalRows() { return totalRows; }
        public int getImportedCount() { return importedCount; }
        public int getRejectedCount() { return errors.size(); }
        public List<RowError> getErrors() { return errors; }
        public List<Customer> getImportedCustomers() { return importedCustomers; }
    }

    public static ImportSummary importCustomers(File csvFile, CustomerService customerService) throws IOException {
        ImportSummary summary = new ImportSummary();
        if (!csvFile.exists()) {
            throw new IOException("CSV file not found at: " + csvFile.getAbsolutePath());
        }

        try (BufferedReader reader = new BufferedReader(new FileReader(csvFile))) {
            String line;
            int lineNumber = 0;

            while ((line = reader.readLine()) != null) {
                lineNumber++;
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith("#")) {
                    continue; // Skip comments and empty lines
                }

                // Check and skip header
                if (lineNumber == 1 && (trimmed.toLowerCase().startsWith("customer_id") ||
                        trimmed.toLowerCase().startsWith("id"))) {
                    continue;
                }

                String[] parts = trimmed.split(",", -1);
                if (parts.length < 5) {
                    summary.recordError(lineNumber, line, "Malformed row: Expected 5 columns, found " + parts.length);
                    continue;
                }

                String id = parts[0].trim();
                String name = parts[1].trim();
                String email = parts[2].trim();
                String phone = parts[3].trim();
                String dobStr = parts[4].trim();

                LocalDate dob;
                try {
                    dob = InputValidator.parseDate(dobStr);
                } catch (DateTimeParseException e) {
                    summary.recordError(lineNumber, line, "Invalid date format for DOB: '" + dobStr + "'. Expected YYYY-MM-DD");
                    continue;
                }

                try {
                    Customer saved = customerService.registerCustomer(id, name, email, phone, dob);
                    summary.recordSuccess(saved);
                } catch (VertexException ex) {
                    summary.recordError(lineNumber, line, ex.getMessage());
                }
            }
        }

        return summary;
    }
}
