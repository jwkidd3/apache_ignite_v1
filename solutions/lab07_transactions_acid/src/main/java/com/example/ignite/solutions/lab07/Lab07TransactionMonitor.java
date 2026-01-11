package com.example.ignite.solutions.lab07;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteTransactions;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheAtomicityMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.transactions.Transaction;
import org.apache.ignite.transactions.TransactionConcurrency;
import org.apache.ignite.transactions.TransactionIsolation;
import org.apache.ignite.transactions.TransactionMetrics;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 07 Challenge 2: Transaction Monitoring Utility
 *
 * Demonstrates:
 * - Real-time transaction metrics monitoring
 * - Transaction throughput tracking
 * - Commit/rollback rate monitoring
 * - Load generation for testing
 */
public class Lab07TransactionMonitor {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Transaction Monitoring Utility ===\n");

            CacheConfiguration<Integer, Integer> cfg =
                new CacheConfiguration<>("monitoredCache");
            cfg.setAtomicityMode(CacheAtomicityMode.TRANSACTIONAL);

            IgniteCache<Integer, Integer> cache = ignite.getOrCreateCache(cfg);
            cache.put(1, 0);

            // Create monitoring utility
            TransactionMonitor monitor = new TransactionMonitor(ignite);

            // Start monitoring thread
            Thread monitorThread = new Thread(() -> {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        monitor.printMetrics();
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            }, "MonitorThread");
            monitorThread.start();

            // Generate transaction load
            System.out.println("Generating transaction load...\n");
            generateLoad(ignite, cache);

            // Final metrics
            Thread.sleep(1000);
            monitorThread.interrupt();
            monitorThread.join();

            System.out.println("\n=== Final Metrics ===");
            monitor.printDetailedMetrics();

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void generateLoad(Ignite ignite, IgniteCache<Integer, Integer> cache) {
        int numThreads = 5;
        int transactionsPerThread = 20;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < numThreads; i++) {
            final int threadNum = i;
            executor.submit(() -> {
                for (int j = 0; j < transactionsPerThread; j++) {
                    try (Transaction tx = ignite.transactions().txStart(
                            TransactionConcurrency.PESSIMISTIC,
                            TransactionIsolation.REPEATABLE_READ,
                            1000, 0)) {

                        Integer value = cache.get(1);
                        Thread.sleep(10 + (int)(Math.random() * 20));
                        cache.put(1, value + 1);

                        if (Math.random() > 0.9) {
                            tx.rollback();
                            failCount.incrementAndGet();
                        } else {
                            tx.commit();
                            successCount.incrementAndGet();
                        }

                    } catch (Exception e) {
                        failCount.incrementAndGet();
                    }
                }
                latch.countDown();
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        executor.shutdown();
        System.out.println("\nLoad generation complete.");
        System.out.println("  Successful transactions: " + successCount.get());
        System.out.println("  Failed transactions: " + failCount.get());
        System.out.println("  Final counter value: " + cache.get(1));
    }

    static class TransactionMonitor {
        private final Ignite ignite;
        private long lastCommits = 0;
        private long lastRollbacks = 0;
        private long lastTimestamp = System.currentTimeMillis();

        public TransactionMonitor(Ignite ignite) {
            this.ignite = ignite;
        }

        public void printMetrics() {
            IgniteTransactions transactions = ignite.transactions();
            TransactionMetrics metrics = transactions.metrics();

            long currentCommits = metrics.txCommits();
            long currentRollbacks = metrics.txRollbacks();
            long currentTimestamp = System.currentTimeMillis();

            long commitsDelta = currentCommits - lastCommits;
            long rollbacksDelta = currentRollbacks - lastRollbacks;
            long timeDelta = currentTimestamp - lastTimestamp;

            double commitsPerSecond = (commitsDelta * 1000.0) / timeDelta;
            double rollbacksPerSecond = (rollbacksDelta * 1000.0) / timeDelta;

            System.out.println(String.format(
                "[Monitor] Commits: %d (%.1f/s) | Rollbacks: %d (%.1f/s)",
                currentCommits, commitsPerSecond,
                currentRollbacks, rollbacksPerSecond));

            lastCommits = currentCommits;
            lastRollbacks = currentRollbacks;
            lastTimestamp = currentTimestamp;
        }

        public void printDetailedMetrics() {
            TransactionMetrics metrics = ignite.transactions().metrics();

            System.out.println("Total Commits:        " + metrics.txCommits());
            System.out.println("Total Rollbacks:      " + metrics.txRollbacks());
            System.out.println("Commit Time (avg):    " +
                (metrics.commitTime() / Math.max(1, metrics.txCommits())) + "ms");
            System.out.println("Rollback Time (avg):  " +
                (metrics.rollbackTime() / Math.max(1, metrics.txRollbacks())) + "ms");
        }
    }
}
