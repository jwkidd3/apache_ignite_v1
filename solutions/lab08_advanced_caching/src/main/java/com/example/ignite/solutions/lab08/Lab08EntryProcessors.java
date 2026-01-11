package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.cache.CacheEntryProcessor;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.processor.EntryProcessorException;
import javax.cache.processor.MutableEntry;
import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Lab 08 Exercise 4: Cache Entry Processors
 *
 * Demonstrates:
 * - Atomic read-modify-write operations
 * - Server-side processing for reduced network overhead
 * - Conditional updates with entry processors
 * - Batch processing with invokeAll
 */
public class Lab08EntryProcessors {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Entry Processors Lab ===\n");

            CacheConfiguration<String, Integer> cfg =
                new CacheConfiguration<>("processorCache");
            cfg.setAtomicityMode(CacheAtomicityMode.ATOMIC);

            IgniteCache<String, Integer> cache = ignite.getOrCreateCache(cfg);

            // Initialize counters
            cache.put("counter1", 0);
            cache.put("counter2", 0);
            cache.put("counter3", 0);

            System.out.println("=== Scenario 1: Atomic Increment ===");

            // Without entry processor (non-atomic)
            System.out.println("\n1. Without Entry Processor (3 operations):");
            long start = System.currentTimeMillis();

            Integer value = cache.get("counter1");
            value = value + 10;
            cache.put("counter1", value);

            long withoutEP = System.currentTimeMillis() - start;
            System.out.println("   Result: " + cache.get("counter1"));
            System.out.println("   Time: " + withoutEP + " ms");
            System.out.println("   Network round trips: 2 (get + put)");

            // With entry processor (atomic)
            System.out.println("\n2. With Entry Processor (1 operation):");
            start = System.currentTimeMillis();

            cache.invoke("counter2", new IncrementProcessor(), 10);

            long withEP = System.currentTimeMillis() - start;
            System.out.println("   Result: " + cache.get("counter2"));
            System.out.println("   Time: " + withEP + " ms");
            System.out.println("   Network round trips: 1 (invoke)");
            System.out.println("   Atomic: Yes");

            System.out.println("\n=== Scenario 2: Conditional Update ===");

            cache.put("balance", 100);

            // Conditional update using entry processor
            boolean withdrawn = cache.invoke("balance",
                new WithdrawProcessor(), 30);

            System.out.println("Withdraw $30: " + (withdrawn ? "Success" : "Failed"));
            System.out.println("New balance: $" + cache.get("balance"));

            // Try to withdraw more than available
            boolean withdrawn2 = cache.invoke("balance",
                new WithdrawProcessor(), 100);

            System.out.println("\nWithdraw $100: " + (withdrawn2 ? "Success" : "Failed"));
            System.out.println("Balance: $" + cache.get("balance"));

            System.out.println("\n=== Scenario 3: Batch Processing ===");

            cache.put("metric1", 0);
            cache.put("metric2", 0);
            cache.put("metric3", 0);

            // Create keys set
            Set<String> keys = new HashSet<>();
            keys.add("metric1");
            keys.add("metric2");
            keys.add("metric3");

            // Increment all counters atomically
            cache.invokeAll(keys, new IncrementProcessor(), 5);

            System.out.println("Batch increment results (+5 each):");
            System.out.println("  metric1: " + cache.get("metric1"));
            System.out.println("  metric2: " + cache.get("metric2"));
            System.out.println("  metric3: " + cache.get("metric3"));

            System.out.println("\n=== Scenario 4: Complex Entry Processor ===");

            cache.put("inventory", 100);

            // Reserve inventory atomically
            ReservationResult result = cache.invoke("inventory",
                new ReserveInventoryProcessor(), 30);

            System.out.println("Reservation attempt for 30 units:");
            System.out.println("  Success: " + result.success);
            System.out.println("  Remaining: " + result.remainingQuantity);
            System.out.println("  Message: " + result.message);

            System.out.println("\n=== Entry Processor Benefits ===");
            System.out.println("- Atomic operations without explicit transactions");
            System.out.println("- Reduced network overhead (compute at data)");
            System.out.println("- Server-side processing for better performance");
            System.out.println("- Avoid race conditions in read-modify-write");
            System.out.println("- Clean separation of business logic");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Entry processor to increment a value
     */
    static class IncrementProcessor implements CacheEntryProcessor<String, Integer, Integer>, Serializable {
        @Override
        public Integer process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int increment = (args.length > 0) ? (Integer) args[0] : 1;
            Integer current = entry.getValue();
            if (current == null) {
                current = 0;
            }
            Integer newValue = current + increment;
            entry.setValue(newValue);
            return newValue;
        }
    }

    /**
     * Entry processor for conditional withdrawal
     */
    static class WithdrawProcessor implements CacheEntryProcessor<String, Integer, Boolean>, Serializable {
        @Override
        public Boolean process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int amount = (Integer) args[0];
            Integer balance = entry.getValue();

            if (balance != null && balance >= amount) {
                entry.setValue(balance - amount);
                return true;
            }
            return false;
        }
    }

    /**
     * Complex entry processor returning a custom result
     */
    static class ReserveInventoryProcessor
            implements CacheEntryProcessor<String, Integer, ReservationResult>, Serializable {
        @Override
        public ReservationResult process(MutableEntry<String, Integer> entry, Object... args)
                throws EntryProcessorException {
            int requestedQuantity = (Integer) args[0];
            Integer currentInventory = entry.getValue();

            if (currentInventory == null || currentInventory < requestedQuantity) {
                return new ReservationResult(false,
                    currentInventory != null ? currentInventory : 0,
                    "Insufficient inventory");
            }

            int remaining = currentInventory - requestedQuantity;
            entry.setValue(remaining);

            return new ReservationResult(true, remaining,
                "Reserved " + requestedQuantity + " units");
        }
    }

    /**
     * Result object for reservation processor
     */
    static class ReservationResult implements Serializable {
        boolean success;
        int remainingQuantity;
        String message;

        ReservationResult(boolean success, int remainingQuantity, String message) {
            this.success = success;
            this.remainingQuantity = remainingQuantity;
            this.message = message;
        }
    }
}
