package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Lab 07 Exercise 6: Cross-Cache Transactions
 *
 * Demonstrates:
 * - Atomic transactions spanning multiple caches
 * - Cross-cache rollback behavior
 * - Complex multi-step transactions
 * - Atomicity verification
 */
public class Lab07CrossCacheTransactions {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cross-Cache Transactions Lab ===\n");

            // Create multiple transactional caches
            IgniteCache<Integer, Account> accountCache = createCache(ignite,
                "accounts", Account.class);
            IgniteCache<String, OrderRecord> orderCache = createCache(ignite,
                "orders", OrderRecord.class);
            IgniteCache<Integer, InventoryItem> inventoryCache = createCache(ignite,
                "inventory", InventoryItem.class);
            IgniteCache<String, AuditLog> auditCache = createCache(ignite,
                "audit", AuditLog.class);

            // Initialize test data
            initializeData(accountCache, inventoryCache);

            // Part 1: Successful cross-cache transaction
            System.out.println("=== Part 1: Successful Cross-Cache Transaction ===");
            performSuccessfulOrder(ignite, accountCache, orderCache,
                                  inventoryCache, auditCache);

            // Part 2: Failed transaction with rollback
            System.out.println("\n=== Part 2: Cross-Cache Rollback ===");
            demonstrateRollback(ignite, accountCache, orderCache,
                               inventoryCache, auditCache);

            // Part 3: Complex multi-step transaction
            System.out.println("\n=== Part 3: Complex Multi-Step Transaction ===");
            performComplexTransaction(ignite, accountCache, orderCache,
                                      inventoryCache, auditCache);

            // Part 4: Verify atomicity across caches
            System.out.println("\n=== Part 4: Atomicity Verification ===");
            verifyAtomicity(ignite, accountCache, inventoryCache);

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
        cfg.setCacheMode(CacheMode.PARTITIONED);
        cfg.setBackups(1);
        return ignite.getOrCreateCache(cfg);
    }

    private static void initializeData(IgniteCache<Integer, Account> accountCache,
                                        IgniteCache<Integer, InventoryItem> inventoryCache) {
        // Create accounts
        accountCache.put(1, new Account(1, "Alice", 1000.00));
        accountCache.put(2, new Account(2, "Bob", 500.00));
        accountCache.put(3, new Account(3, "Charlie", 2000.00));

        // Create inventory
        inventoryCache.put(101, new InventoryItem(101, "Laptop", 10, 599.99));
        inventoryCache.put(102, new InventoryItem(102, "Mouse", 50, 29.99));
        inventoryCache.put(103, new InventoryItem(103, "Keyboard", 30, 79.99));

        System.out.println("Initial Data:");
        System.out.println("  Accounts: Alice($1000), Bob($500), Charlie($2000)");
        System.out.println("  Inventory: Laptop(10), Mouse(50), Keyboard(30)\n");
    }

    private static void performSuccessfulOrder(Ignite ignite,
                                                IgniteCache<Integer, Account> accountCache,
                                                IgniteCache<String, OrderRecord> orderCache,
                                                IgniteCache<Integer, InventoryItem> inventoryCache,
                                                IgniteCache<String, AuditLog> auditCache) {

        int customerId = 1;
        int productId = 102; // Mouse
        int quantity = 2;

        System.out.println("Alice ordering 2 mice...\n");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Check and update inventory
            InventoryItem item = inventoryCache.get(productId);
            if (item.getQuantity() < quantity) {
                tx.rollback();
                System.out.println("Insufficient inventory!");
                return;
            }

            double totalCost = item.getPrice() * quantity;
            System.out.println("  [Inventory] Reserving " + quantity + " " + item.getName());
            item.setQuantity(item.getQuantity() - quantity);
            inventoryCache.put(productId, item);

            // Step 2: Check and update account
            Account account = accountCache.get(customerId);
            if (account.getBalance() < totalCost) {
                tx.rollback();
                System.out.println("Insufficient funds!");
                return;
            }

            System.out.println("  [Account] Charging $" + totalCost + " to " + account.getName());
            account.setBalance(account.getBalance() - totalCost);
            accountCache.put(customerId, account);

            // Step 3: Create order record
            String orderId = UUID.randomUUID().toString().substring(0, 8);
            OrderRecord order = new OrderRecord(orderId, customerId, productId,
                                                quantity, totalCost, "COMPLETED");
            System.out.println("  [Order] Creating order " + orderId);
            orderCache.put(orderId, order);

            // Step 4: Create audit log
            String auditId = UUID.randomUUID().toString().substring(0, 8);
            AuditLog audit = new AuditLog(auditId, "ORDER_PLACED",
                "Customer " + customerId + " ordered " + quantity + " x " + item.getName());
            System.out.println("  [Audit] Recording transaction");
            auditCache.put(auditId, audit);

            // Commit all changes atomically
            tx.commit();
            System.out.println("\nTransaction committed successfully!");
            System.out.println("  New balance: $" + accountCache.get(customerId).getBalance());
            System.out.println("  New inventory: " + inventoryCache.get(productId).getQuantity() +
                             " " + item.getName() + "(s)");

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }

    private static void demonstrateRollback(Ignite ignite,
                                            IgniteCache<Integer, Account> accountCache,
                                            IgniteCache<String, OrderRecord> orderCache,
                                            IgniteCache<Integer, InventoryItem> inventoryCache,
                                            IgniteCache<String, AuditLog> auditCache) {

        int customerId = 2; // Bob with $500
        int productId = 101; // Laptop at $599.99
        int quantity = 1;

        System.out.println("Bob attempting to order 1 laptop ($599.99)...");
        System.out.println("Bob's balance: $" + accountCache.get(customerId).getBalance() + "\n");

        // Store initial states for verification
        double initialBalance = accountCache.get(customerId).getBalance();
        int initialInventory = inventoryCache.get(productId).getQuantity();

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Update inventory (this will succeed)
            InventoryItem item = inventoryCache.get(productId);
            double totalCost = item.getPrice() * quantity;

            System.out.println("  [Inventory] Reserving " + quantity + " " + item.getName());
            item.setQuantity(item.getQuantity() - quantity);
            inventoryCache.put(productId, item);

            // Step 2: Try to update account (this will fail)
            Account account = accountCache.get(customerId);
            System.out.println("  [Account] Checking balance: $" + account.getBalance());

            if (account.getBalance() < totalCost) {
                System.out.println("  [Account] INSUFFICIENT FUNDS!");
                System.out.println("  [Transaction] Rolling back ALL changes...");
                tx.rollback();

                // Inventory should be restored!
                System.out.println("\nRollback completed!");
                return;
            }

            account.setBalance(account.getBalance() - totalCost);
            accountCache.put(customerId, account);

            tx.commit();

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }

        // Verify rollback worked
        double finalBalance = accountCache.get(customerId).getBalance();
        int finalInventory = inventoryCache.get(productId).getQuantity();

        System.out.println("\nVerifying rollback:");
        System.out.println("  Account balance: $" + initialBalance + " -> $" + finalBalance +
                         (initialBalance == finalBalance ? " (UNCHANGED)" : " (CHANGED!)"));
        System.out.println("  Laptop inventory: " + initialInventory + " -> " + finalInventory +
                         (initialInventory == finalInventory ? " (UNCHANGED)" : " (CHANGED!)"));

        if (initialBalance == finalBalance && initialInventory == finalInventory) {
            System.out.println("\n  ATOMICITY VERIFIED: All caches rolled back together!");
        }
    }

    private static void performComplexTransaction(Ignite ignite,
                                                   IgniteCache<Integer, Account> accountCache,
                                                   IgniteCache<String, OrderRecord> orderCache,
                                                   IgniteCache<Integer, InventoryItem> inventoryCache,
                                                   IgniteCache<String, AuditLog> auditCache) {

        System.out.println("Complex transaction: Transfer + Purchase + Logging\n");

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Step 1: Transfer money from Charlie to Alice
            Account charlie = accountCache.get(3);
            Account alice = accountCache.get(1);
            double transferAmount = 100.00;

            System.out.println("  Step 1: Transfer $" + transferAmount +
                             " from Charlie to Alice");
            charlie.setBalance(charlie.getBalance() - transferAmount);
            alice.setBalance(alice.getBalance() + transferAmount);
            accountCache.put(3, charlie);
            accountCache.put(1, alice);

            // Log the transfer
            String transferAuditId = UUID.randomUUID().toString().substring(0, 8);
            auditCache.put(transferAuditId, new AuditLog(transferAuditId, "TRANSFER",
                "Charlie -> Alice: $" + transferAmount));

            // Step 2: Alice purchases a keyboard
            int productId = 103;
            InventoryItem keyboard = inventoryCache.get(productId);
            double cost = keyboard.getPrice();

            System.out.println("  Step 2: Alice purchases keyboard ($" + cost + ")");
            keyboard.setQuantity(keyboard.getQuantity() - 1);
            inventoryCache.put(productId, keyboard);

            alice = accountCache.get(1); // Re-read to get updated balance
            alice.setBalance(alice.getBalance() - cost);
            accountCache.put(1, alice);

            // Create order
            String orderId = UUID.randomUUID().toString().substring(0, 8);
            orderCache.put(orderId, new OrderRecord(orderId, 1, productId, 1, cost, "COMPLETED"));

            // Log the purchase
            String purchaseAuditId = UUID.randomUUID().toString().substring(0, 8);
            auditCache.put(purchaseAuditId, new AuditLog(purchaseAuditId, "PURCHASE",
                "Alice bought keyboard for $" + cost));

            System.out.println("  Step 3: Recording audit logs");

            tx.commit();
            System.out.println("\nComplex transaction committed!");
            System.out.println("  Alice's final balance: $" + accountCache.get(1).getBalance());
            System.out.println("  Charlie's final balance: $" + accountCache.get(3).getBalance());
            System.out.println("  Keyboards remaining: " + inventoryCache.get(103).getQuantity());

        } catch (Exception e) {
            System.out.println("Transaction failed: " + e.getMessage());
        }
    }

    private static void verifyAtomicity(Ignite ignite,
                                        IgniteCache<Integer, Account> accountCache,
                                        IgniteCache<Integer, InventoryItem> inventoryCache) {

        System.out.println("Testing atomicity with simulated failure...\n");

        // Store initial states
        double aliceBalance = accountCache.get(1).getBalance();
        int laptopQty = inventoryCache.get(101).getQuantity();

        System.out.println("Before transaction:");
        System.out.println("  Alice's balance: $" + aliceBalance);
        System.out.println("  Laptop quantity: " + laptopQty);

        try (Transaction tx = ignite.transactions().txStart(
                TransactionConcurrency.PESSIMISTIC,
                TransactionIsolation.REPEATABLE_READ)) {

            // Modify account
            Account alice = accountCache.get(1);
            alice.setBalance(alice.getBalance() - 100);
            accountCache.put(1, alice);
            System.out.println("\n  Modified account (not yet committed)");

            // Modify inventory
            InventoryItem laptop = inventoryCache.get(101);
            laptop.setQuantity(laptop.getQuantity() - 1);
            inventoryCache.put(101, laptop);
            System.out.println("  Modified inventory (not yet committed)");

            // Simulate failure before commit
            System.out.println("\n  Simulating failure...");
            throw new RuntimeException("Simulated system failure!");

        } catch (RuntimeException e) {
            System.out.println("  Exception: " + e.getMessage());
            System.out.println("  Transaction automatically rolled back");
        }

        // Verify nothing changed
        double finalBalance = accountCache.get(1).getBalance();
        int finalQty = inventoryCache.get(101).getQuantity();

        System.out.println("\nAfter rollback:");
        System.out.println("  Alice's balance: $" + finalBalance +
                         (aliceBalance == finalBalance ? " (UNCHANGED)" : " (CHANGED!)"));
        System.out.println("  Laptop quantity: " + finalQty +
                         (laptopQty == finalQty ? " (UNCHANGED)" : " (CHANGED!)"));

        if (aliceBalance == finalBalance && laptopQty == finalQty) {
            System.out.println("\n  CROSS-CACHE ATOMICITY CONFIRMED!");
            System.out.println("  Both caches remain consistent after rollback.");
        }
    }

    // Domain classes
    static class Account implements Serializable {
        private int id;
        private String name;
        private double balance;

        public Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public double getBalance() { return balance; }
        public void setBalance(double balance) { this.balance = balance; }
    }

    static class InventoryItem implements Serializable {
        private int id;
        private String name;
        private int quantity;
        private double price;

        public InventoryItem(int id, String name, int quantity, double price) {
            this.id = id;
            this.name = name;
            this.quantity = quantity;
            this.price = price;
        }

        public int getId() { return id; }
        public String getName() { return name; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { this.quantity = quantity; }
        public double getPrice() { return price; }
    }

    static class OrderRecord implements Serializable {
        private String orderId;
        private int customerId;
        private int productId;
        private int quantity;
        private double total;
        private String status;

        public OrderRecord(String orderId, int customerId, int productId,
                          int quantity, double total, String status) {
            this.orderId = orderId;
            this.customerId = customerId;
            this.productId = productId;
            this.quantity = quantity;
            this.total = total;
            this.status = status;
        }

        public String getOrderId() { return orderId; }
    }

    static class AuditLog implements Serializable {
        private String id;
        private String action;
        private String details;
        private String timestamp;

        public AuditLog(String id, String action, String details) {
            this.id = id;
            this.action = action;
            this.details = details;
            this.timestamp = LocalDateTime.now()
                .format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        }

        public String getId() { return id; }
        public String getAction() { return action; }
        public String getDetails() { return details; }
    }
}
