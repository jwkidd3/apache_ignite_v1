package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;

/**
 * Lab 07 Exercise 1: Basic Transactions
 *
 * Demonstrates fundamental transactional operations in Apache Ignite:
 * - Creating transactional caches
 * - Successful transaction commit
 * - Transaction rollback
 * - Exception handling with automatic rollback
 */
public class Lab07BasicTransactions {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Basic Transactions Lab ===\n");

            // Create transactional cache
            CacheConfiguration<Integer, Integer> cfg =
                new CacheConfiguration<>("accountCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);
            cfg.setBackups(1);

            IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);

            // Initialize accounts
            cache.put(1, 1000); // Account 1: $1000
            cache.put(2, 500);  // Account 2: $500

            System.out.println("Initial balances:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Successful transaction
            System.out.println("\n=== Test 1: Successful Transaction ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                int balance2 = cache.get(2);

                // Transfer $200 from account 1 to account 2
                cache.put(1, balance1 - 200);
                cache.put(2, balance2 + 200);

                tx.commit();
                System.out.println("Transaction committed successfully");
            }

            System.out.println("New balances:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Failed transaction (rollback)
            System.out.println("\n=== Test 2: Transaction Rollback ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                int balance2 = cache.get(2);

                // Try to transfer more than available
                if (balance1 < 2000) {
                    System.out.println("Insufficient funds! Rolling back...");
                    tx.rollback();
                } else {
                    cache.put(1, balance1 - 2000);
                    cache.put(2, balance2 + 2000);
                    tx.commit();
                }
            }

            System.out.println("Balances after rollback:");
            System.out.println("Account 1: $" + cache.get(1));
            System.out.println("Account 2: $" + cache.get(2));

            // Transaction with exception (automatic rollback)
            System.out.println("\n=== Test 3: Exception Handling ===");
            try (Transaction tx = ignite.transactions().txStart(
                    TransactionConcurrency.PESSIMISTIC,
                    TransactionIsolation.REPEATABLE_READ)) {

                int balance1 = cache.get(1);
                cache.put(1, balance1 - 100);

                // Simulate error
                if (true) {
                    throw new RuntimeException("Simulated error");
                }

                tx.commit();

            } catch (Exception e) {
                System.out.println("Exception caught: " + e.getMessage());
                System.out.println("Transaction automatically rolled back");
            }

            System.out.println("Balance after exception:");
            System.out.println("Account 1: $" + cache.get(1) + " (unchanged)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
