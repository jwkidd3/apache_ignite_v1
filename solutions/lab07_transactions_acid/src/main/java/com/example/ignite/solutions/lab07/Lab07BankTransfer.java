package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Lab 07 Challenge 1: Bank Transfer with Audit Log
 *
 * Demonstrates a complete bank transfer system with:
 * - ACID-compliant transfers
 * - Consistent lock ordering to prevent deadlocks
 * - Full audit trail
 * - Transfer history tracking
 */
public class Lab07BankTransfer {

    private static final AtomicLong transactionCounter = new AtomicLong(0);

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Bank Transfer Challenge ===\n");

            // Create caches
            IgniteCache<String, BankAccount> accounts = createCache(ignite,
                "bankAccounts", BankAccount.class);
            IgniteCache<Long, TransferRecord> transfers = createCache(ignite,
                "transferRecords", TransferRecord.class);
            IgniteCache<Long, AuditEntry> auditLog = createCache(ignite,
                "bankAudit", AuditEntry.class);

            // Initialize accounts
            accounts.put("ACC001", new BankAccount("ACC001", "John Doe", 5000.00));
            accounts.put("ACC002", new BankAccount("ACC002", "Jane Smith", 3000.00));
            accounts.put("ACC003", new BankAccount("ACC003", "Bob Wilson", 1000.00));

            System.out.println("Initial Account Balances:");
            printAllAccounts(accounts);

            // Create transfer service
            TransferService service = new TransferService(ignite, accounts,
                                                          transfers, auditLog);

            // Test successful transfer
            System.out.println("\n=== Test 1: Successful Transfer ===");
            TransferResult result1 = service.transfer("ACC001", "ACC002", 500.00,
                                                       "Payment for services");
            System.out.println("Result: " + result1);
            printAllAccounts(accounts);

            // Test insufficient funds
            System.out.println("\n=== Test 2: Insufficient Funds ===");
            TransferResult result2 = service.transfer("ACC003", "ACC001", 2000.00,
                                                       "Large payment");
            System.out.println("Result: " + result2);
            printAllAccounts(accounts);

            // Test same account transfer
            System.out.println("\n=== Test 3: Same Account Transfer ===");
            TransferResult result3 = service.transfer("ACC001", "ACC001", 100.00,
                                                       "Self transfer");
            System.out.println("Result: " + result3);

            // Print audit log
            System.out.println("\n=== Audit Log ===");
            auditLog.forEach(entry -> {
                AuditEntry audit = entry.getValue();
                System.out.println(String.format("  [%s] %s - %s",
                    audit.getTimestamp(), audit.getAction(), audit.getDetails()));
            });

            // Print transfer history
            System.out.println("\n=== Transfer History ===");
            transfers.forEach(entry -> {
                TransferRecord transfer = entry.getValue();
                System.out.println(String.format("  #%d: %s -> %s, $%.2f (%s)",
                    transfer.getId(), transfer.getFromAccount(),
                    transfer.getToAccount(), transfer.getAmount(),
                    transfer.getStatus()));
            });

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static <K, V> IgniteCache<K, V> createCache(Ignite ignite,
                                                         String name,
                                                         Class<V> valueClass) {
        CacheConfiguration<K, V> cfg = new CacheConfiguration<>(name);
        cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
        return ignite.getOrCreateCache(cfg);
    }

    private static void printAllAccounts(IgniteCache<String, BankAccount> accounts) {
        accounts.forEach(entry -> {
            BankAccount acc = entry.getValue();
            System.out.println(String.format("  %s (%s): $%.2f",
                acc.getAccountNumber(), acc.getOwnerName(), acc.getBalance()));
        });
    }

    static class TransferService {
        private final Ignite ignite;
        private final IgniteCache<String, BankAccount> accounts;
        private final IgniteCache<Long, TransferRecord> transfers;
        private final IgniteCache<Long, AuditEntry> auditLog;

        public TransferService(Ignite ignite,
                              IgniteCache<String, BankAccount> accounts,
                              IgniteCache<Long, TransferRecord> transfers,
                              IgniteCache<Long, AuditEntry> auditLog) {
            this.ignite = ignite;
            this.accounts = accounts;
            this.transfers = transfers;
            this.auditLog = auditLog;
        }

        public TransferResult transfer(String fromAcct, String toAcct,
                                       double amount, String description) {
            // Validate input
            if (fromAcct.equals(toAcct)) {
                return new TransferResult(false, "Cannot transfer to same account");
            }
            if (amount <= 0) {
                return new TransferResult(false, "Amount must be positive");
            }

            long transferId = transactionCounter.incrementAndGet();
            String timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);

            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ,
                    5000, 0)) {

                // Lock accounts in consistent order to prevent deadlocks
                String firstAcct = fromAcct.compareTo(toAcct) < 0 ? fromAcct : toAcct;
                String secondAcct = fromAcct.compareTo(toAcct) < 0 ? toAcct : fromAcct;

                BankAccount first = accounts.get(firstAcct);
                BankAccount second = accounts.get(secondAcct);

                if (first == null || second == null) {
                    tx.rollback();
                    recordAudit("TRANSFER_FAILED",
                        "Account not found: " + (first == null ? firstAcct : secondAcct));
                    return new TransferResult(false, "Account not found");
                }

                BankAccount source = fromAcct.equals(firstAcct) ? first : second;
                BankAccount target = toAcct.equals(firstAcct) ? first : second;

                // Check balance
                if (source.getBalance() < amount) {
                    tx.rollback();
                    recordTransfer(transferId, fromAcct, toAcct, amount,
                                  description, "FAILED_INSUFFICIENT_FUNDS");
                    recordAudit("TRANSFER_FAILED",
                        String.format("Insufficient funds: %s has $%.2f, needed $%.2f",
                            fromAcct, source.getBalance(), amount));
                    return new TransferResult(false, "Insufficient funds");
                }

                // Perform transfer
                source.setBalance(source.getBalance() - amount);
                target.setBalance(target.getBalance() + amount);

                accounts.put(source.getAccountNumber(), source);
                accounts.put(target.getAccountNumber(), target);

                // Record transfer
                recordTransfer(transferId, fromAcct, toAcct, amount,
                              description, "COMPLETED");

                // Record audit entries
                recordAudit("DEBIT", String.format("%s debited $%.2f for: %s",
                    fromAcct, amount, description));
                recordAudit("CREDIT", String.format("%s credited $%.2f from: %s",
                    toAcct, amount, fromAcct));
                recordAudit("TRANSFER_COMPLETED",
                    String.format("Transfer #%d: $%.2f from %s to %s",
                        transferId, amount, fromAcct, toAcct));

                tx.commit();
                return new TransferResult(true, "Transfer completed successfully",
                                         transferId);

            } catch (Exception e) {
                recordAudit("TRANSFER_ERROR",
                    String.format("Transfer failed with error: %s", e.getMessage()));
                return new TransferResult(false, "Transfer failed: " + e.getMessage());
            }
        }

        private void recordTransfer(long id, String from, String to,
                                    double amount, String desc, String status) {
            transfers.put(id, new TransferRecord(id, from, to, amount, desc, status));
        }

        private void recordAudit(String action, String details) {
            long auditId = System.nanoTime();
            auditLog.put(auditId, new AuditEntry(auditId, action, details));
        }
    }

    static class BankAccount implements Serializable {
        private String accountNumber;
        private String ownerName;
        private double balance;

        public BankAccount(String accountNumber, String ownerName, double balance) {
            this.accountNumber = accountNumber;
            this.ownerName = ownerName;
            this.balance = balance;
        }

        public String getAccountNumber() { return accountNumber; }
        public String getOwnerName() { return ownerName; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
    }

    static class TransferRecord implements Serializable {
        private long id;
        private String fromAccount;
        private String toAccount;
        private double amount;
        private String description;
        private String status;
        private String timestamp;

        public TransferRecord(long id, String from, String to,
                             double amount, String desc, String status) {
            this.id = id;
            this.fromAccount = from;
            this.toAccount = to;
            this.amount = amount;
            this.description = desc;
            this.status = status;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public long getId() { return id; }
        public String getFromAccount() { return fromAccount; }
        public String getToAccount() { return toAccount; }
        public double getAmount() { return amount; }
        public String getStatus() { return status; }
    }

    static class AuditEntry implements Serializable {
        private long id;
        private String action;
        private String details;
        private String timestamp;

        public AuditEntry(long id, String action, String details) {
            this.id = id;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String getAction() { return action; }
        public String getDetails() { return details; }
        public String getTimestamp() { return timestamp; }
    }

    static class TransferResult {
        private boolean success;
        private String message;
        private Long transferId;

        public TransferResult(boolean success, String message) {
            this(success, message, null);
        }

        public TransferResult(boolean success, String message, Long transferId) {
            this.success = success;
            this.message = message;
            this.transferId = transferId;
        }

        @Override
        public String toString() {
            return String.format("%s: %s%s",
                success ? "SUCCESS" : "FAILED",
                message,
                transferId != null ? " (Transfer #" + transferId + ")" : "");
        }
    }
}
