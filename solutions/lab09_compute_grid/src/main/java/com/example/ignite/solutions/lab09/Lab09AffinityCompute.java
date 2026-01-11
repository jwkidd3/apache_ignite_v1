package com.example.ignite.solutions.lab09;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.IgniteCompute;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteCallable;
import org.apache.ignite.lang.IgniteRunnable;
import org.apache.ignite.resources.IgniteInstanceResource;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;

/**
 * Lab 09 Exercise 5: Affinity Computing
 *
 * Demonstrates:
 * - affinityCall for data-local computation
 * - affinityRun for data-local updates
 * - Multi-cache affinity operations
 * - Performance benefits of data locality
 */
public class Lab09AffinityCompute {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Affinity Computing Lab ===\n");

            // Create partitioned cache
            CacheConfiguration<Integer, Account> cfg =
                new CacheConfiguration<>("accountCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setBackups(1);

            IgniteCache<Integer, Account> cache = ignite.getOrCreateCache(cfg);

            // Populate accounts
            for (int i = 1; i <= 100; i++) {
                cache.put(i, new Account(i, "Account-" + i, 1000.0 * i));
            }

            System.out.println("Loaded 100 accounts\n");

            IgniteCompute compute = ignite.compute();

            // ============================================
            // Compare non-affinity vs affinity calls
            // ============================================

            System.out.println("=== Performance Comparison ===\n");

            // Non-affinity approach (may cause network hops)
            System.out.println("1. Without Affinity (may cause network hops):");
            long startNonAffinity = System.currentTimeMillis();
            double totalNonAffinity = 0.0;

            for (int i = 1; i <= 100; i++) {
                final int accountId = i;
                Double balance = compute.call(() -> {
                    IgniteCache<Integer, Account> c = Ignition.ignite().cache("accountCache");
                    Account account = c.get(accountId);
                    return account != null ? account.getBalance() : 0.0;
                });
                totalNonAffinity += balance;
            }

            long nonAffinityTime = System.currentTimeMillis() - startNonAffinity;
            System.out.println("Time: " + nonAffinityTime + " ms");
            System.out.println("Total balance: $" + String.format("%.2f", totalNonAffinity));

            // Affinity approach (guaranteed local access)
            System.out.println("\n2. With Affinity (guaranteed local access):");
            long startAffinity = System.currentTimeMillis();
            double totalAffinity = 0.0;

            for (int i = 1; i <= 100; i++) {
                final int accountId = i;
                Double balance = compute.affinityCall("accountCache", accountId,
                    new IgniteCallable<Double>() {
                        @IgniteInstanceResource
                        private Ignite ignite;

                        @Override
                        public Double call() {
                            IgniteCache<Integer, Account> c = ignite.cache("accountCache");
                            // localPeek - no network hop, data is guaranteed local
                            Account account = c.localPeek(accountId);
                            return account != null ? account.getBalance() : 0.0;
                        }
                    });
                totalAffinity += balance;
            }

            long affinityTime = System.currentTimeMillis() - startAffinity;
            System.out.println("Time: " + affinityTime + " ms");
            System.out.println("Total balance: $" + String.format("%.2f", totalAffinity));

            // Performance comparison
            System.out.println("\n--- Performance Summary ---");
            System.out.println("Non-affinity time: " + nonAffinityTime + " ms");
            System.out.println("Affinity time:     " + affinityTime + " ms");
            if (affinityTime > 0) {
                System.out.printf("Speedup:           %.2fx%n",
                    (double) nonAffinityTime / affinityTime);
            }

            // ============================================
            // affinityRun Example: Update Operations
            // ============================================

            System.out.println("\n=== affinityRun Example: Account Updates ===");

            // Apply interest to specific accounts using affinity
            int[] accountsToUpdate = {10, 20, 30, 40, 50};
            double interestRate = 0.05;

            for (int accountId : accountsToUpdate) {
                compute.affinityRun("accountCache", accountId, new IgniteRunnable() {
                    @IgniteInstanceResource
                    private Ignite ignite;

                    @Override
                    public void run() {
                        IgniteCache<Integer, Account> c = ignite.cache("accountCache");
                        Account account = c.localPeek(accountId);

                        if (account != null) {
                            double newBalance = account.getBalance() * (1 + interestRate);
                            account.setBalance(newBalance);
                            c.put(accountId, account);

                            System.out.println("Updated account " + accountId +
                                " on node " + ignite.cluster().localNode().consistentId() +
                                " - New balance: $" + String.format("%.2f", newBalance));
                        }
                    }
                });
            }

            Thread.sleep(500);

            // ============================================
            // Batch Affinity Operations
            // ============================================

            System.out.println("\n=== Batch Affinity Operations ===");

            // Group keys by affinity partition for batch processing
            Affinity<Integer> affinity = ignite.affinity("accountCache");

            System.out.println("Partition distribution:");
            for (int partition = 0; partition < Math.min(5, affinity.partitions()); partition++) {
                ClusterNode primary = affinity.mapPartitionToNode(partition);
                System.out.println("  Partition " + partition + " -> Node " +
                    primary.consistentId());
            }

            // Process all accounts in a specific partition
            int targetPartition = 0;
            System.out.println("\nProcessing all accounts in partition " + targetPartition);

            Collection<Integer> partitionKeys = new ArrayList<>();
            for (int i = 1; i <= 1000; i++) {
                if (affinity.partition(i) == targetPartition) {
                    partitionKeys.add(i);
                }
            }
            System.out.println("Found " + partitionKeys.size() + " accounts in partition " + targetPartition);

            // ============================================
            // Multiple Cache Affinity
            // ============================================

            System.out.println("\n=== Multiple Cache Affinity ===");

            // Create related cache with same affinity
            CacheConfiguration<Integer, Transaction> txCfg =
                new CacheConfiguration<>("transactionCache");
            txCfg.setCacheMode(CacheMode.PARTITIONED);
            txCfg.setBackups(1);

            IgniteCache<Integer, Transaction> txCache = ignite.getOrCreateCache(txCfg);

            // Add some transactions
            for (int i = 1; i <= 100; i++) {
                txCache.put(i, new Transaction(i, i, 50.0, "Purchase"));
            }

            // Process account and its transactions together using affinity
            int accountId = 25;
            compute.affinityRun(
                Arrays.asList("accountCache", "transactionCache"),  // Multiple caches
                accountId,
                new IgniteRunnable() {
                    @IgniteInstanceResource
                    private Ignite ignite;

                    @Override
                    public void run() {
                        IgniteCache<Integer, Account> accounts = ignite.cache("accountCache");
                        IgniteCache<Integer, Transaction> transactions = ignite.cache("transactionCache");

                        Account account = accounts.localPeek(accountId);
                        Transaction tx = transactions.localPeek(accountId);

                        if (account != null && tx != null) {
                            System.out.println("Processing locally on node " +
                                ignite.cluster().localNode().consistentId());
                            System.out.println("  Account: " + account.getName() +
                                " Balance: $" + account.getBalance());
                            System.out.println("  Transaction: " + tx.getDescription() +
                                " Amount: $" + tx.getAmount());
                        }
                    }
                }
            );

            Thread.sleep(500);

            System.out.println("\n=== Affinity Computing Best Practices ===");
            System.out.println("1. Use affinityRun/affinityCall for data-intensive tasks");
            System.out.println("2. Use localPeek() instead of get() for guaranteed local access");
            System.out.println("3. Colocate related data using affinity keys");
            System.out.println("4. Batch operations by partition for efficiency");
            System.out.println("5. Monitor network traffic to verify data locality");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    static class Account implements Serializable {
        private int id;
        private String name;
        private double balance;

        Account(int id, String name, double balance) {
            this.id = id;
            this.name = name;
            this.balance = balance;
        }

        int getId() { return id; }
        String getName() { return name; }
        double getBalance() { return balance; }
        void setBalance(double balance) { this.balance = balance; }
    }

    static class Transaction implements Serializable {
        private int id;
        private int accountId;
        private double amount;
        private String description;

        Transaction(int id, int accountId, double amount, String description) {
            this.id = id;
            this.accountId = accountId;
            this.amount = amount;
            this.description = description;
        }

        int getId() { return id; }
        int getAccountId() { return accountId; }
        double getAmount() { return amount; }
        String getDescription() { return description; }
    }
}
