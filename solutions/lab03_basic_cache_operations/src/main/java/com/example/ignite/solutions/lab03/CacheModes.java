package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

/**
 * Lab 3 Exercise 2: Cache Modes
 *
 * This exercise demonstrates different cache modes:
 * PARTITIONED and REPLICATED.
 *
 * Note: LOCAL mode was deprecated and removed in Ignite 2.x.
 * For node-local data, use a PARTITIONED cache with 0 backups.
 */
public class CacheModes {

    public static void main(String[] args) {
        int nodeNumber = 1;
        if (args.length >= 1) {
            nodeNumber = Integer.parseInt(args[0]);
        }

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-mode-node-" + nodeNumber);

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Cache Modes Lab - Node " + nodeNumber + " ===\n");

            // 1. PARTITIONED Cache
            CacheConfiguration<Integer, String> partitionedCfg =
                new CacheConfiguration<>("partitionedCache");
            partitionedCfg.setCacheMode(CacheMode.PARTITIONED);
            partitionedCfg.setBackups(1); // Number of backup copies

            IgniteCache<Integer, String> partitionedCache =
                ignite.getOrCreateCache(partitionedCfg);

            // 2. REPLICATED Cache
            CacheConfiguration<Integer, String> replicatedCfg =
                new CacheConfiguration<>("replicatedCache");
            replicatedCfg.setCacheMode(CacheMode.REPLICATED);

            IgniteCache<Integer, String> replicatedCache =
                ignite.getOrCreateCache(replicatedCfg);

            // 3. PARTITIONED with no backups (node-local behavior simulation)
            CacheConfiguration<Integer, String> noBackupCfg =
                new CacheConfiguration<>("noBackupCache");
            noBackupCfg.setCacheMode(CacheMode.PARTITIONED);
            noBackupCfg.setBackups(0); // No backups - data only on primary node

            IgniteCache<Integer, String> noBackupCache =
                ignite.getOrCreateCache(noBackupCfg);

            // Populate caches (only from node 1)
            if (nodeNumber == 1) {
                System.out.println("Node 1: Populating caches...\n");

                for (int i = 1; i <= 10; i++) {
                    partitionedCache.put(i, "Partitioned-" + i);
                    replicatedCache.put(i, "Replicated-" + i);
                    noBackupCache.put(i, "NoBackup-" + i);
                }
                System.out.println("Added 10 entries to each cache");
            }

            // Wait for data to propagate
            Thread.sleep(1000);

            // Check cache sizes on each node
            System.out.println("\n=== Cache Sizes on Node " + nodeNumber + " ===");
            System.out.println("Partitioned cache size: " + partitionedCache.size());
            System.out.println("Replicated cache size: " + replicatedCache.size());
            System.out.println("No-backup cache size: " + noBackupCache.size());

            // Check local sizes (data actually stored on this node)
            System.out.println("\n=== Local Cache Sizes (data on this node) ===");
            System.out.println("Partitioned local size: " +
                partitionedCache.localSize());
            System.out.println("Replicated local size: " +
                replicatedCache.localSize());
            System.out.println("No-backup local size: " +
                noBackupCache.localSize());

            // Demonstrate data access
            System.out.println("\n=== Data Access Test ===");
            System.out.println("Partitioned cache key 1: " +
                partitionedCache.get(1));
            System.out.println("Replicated cache key 1: " +
                replicatedCache.get(1));
            System.out.println("No-backup cache key 1: " +
                noBackupCache.get(1));

            System.out.println("\n=== Cache Mode Characteristics ===");
            System.out.println("PARTITIONED: Data distributed across nodes, " +
                "with configurable backups");
            System.out.println("  - Best for: Large datasets, scalability");
            System.out.println("  - Memory usage: Divided across nodes");

            System.out.println("\nREPLICATED: Full copy of data on every node");
            System.out.println("  - Best for: Small, read-heavy datasets");
            System.out.println("  - Memory usage: Full dataset per node");

            System.out.println("\nPARTITIONED (0 backups): Data only on primary node");
            System.out.println("  - Best for: Temporary data, no fault tolerance needed");
            System.out.println("  - Memory usage: Minimal, but no redundancy");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
