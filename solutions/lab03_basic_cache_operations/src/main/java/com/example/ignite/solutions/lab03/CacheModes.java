package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Lab 3 Exercise 2: Cache Modes
 *
 * Demonstrates different cache modes: PARTITIONED and REPLICATED.
 * Starts 3 nodes in the same JVM to show data distribution.
 */
public class CacheModes {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
    }

    public static void main(String[] args) {
        // Start 3 server nodes
        Ignite node1 = startNode(1);
        Ignite node2 = startNode(2);
        Ignite node3 = startNode(3);

        try {
            // PARTITIONED Cache (1 backup)
            CacheConfiguration<Integer, String> partitionedCfg =
                new CacheConfiguration<>("partitionedCache");
            partitionedCfg.setCacheMode(CacheMode.PARTITIONED);
            partitionedCfg.setBackups(1);

            IgniteCache<Integer, String> partitionedCache =
                node1.getOrCreateCache(partitionedCfg);

            // REPLICATED Cache
            CacheConfiguration<Integer, String> replicatedCfg =
                new CacheConfiguration<>("replicatedCache");
            replicatedCfg.setCacheMode(CacheMode.REPLICATED);

            IgniteCache<Integer, String> replicatedCache =
                node1.getOrCreateCache(replicatedCfg);

            // PARTITIONED with no backups
            CacheConfiguration<Integer, String> noBackupCfg =
                new CacheConfiguration<>("noBackupCache");
            noBackupCfg.setCacheMode(CacheMode.PARTITIONED);
            noBackupCfg.setBackups(0);

            IgniteCache<Integer, String> noBackupCache =
                node1.getOrCreateCache(noBackupCfg);

            // Populate caches
            for (int i = 1; i <= 10; i++) {
                partitionedCache.put(i, "Partitioned-" + i);
                replicatedCache.put(i, "Replicated-" + i);
                noBackupCache.put(i, "NoBackup-" + i);
            }

            // Wait for rebalancing
            Thread.sleep(3000);

            // Get caches from other nodes
            IgniteCache<Integer, String> partitionedNode2 = node2.cache("partitionedCache");
            IgniteCache<Integer, String> replicatedNode2 = node2.cache("replicatedCache");
            IgniteCache<Integer, String> noBackupNode2 = node2.cache("noBackupCache");

            IgniteCache<Integer, String> partitionedNode3 = node3.cache("partitionedCache");
            IgniteCache<Integer, String> replicatedNode3 = node3.cache("replicatedCache");
            IgniteCache<Integer, String> noBackupNode3 = node3.cache("noBackupCache");

            // Display cache information
            System.out.println("\n=== Cache Modes Demo (3 nodes) ===\n");

            System.out.println("=== Cluster-Wide Cache Sizes ===");
            System.out.println("Partitioned (1 backup): " + partitionedCache.size());
            System.out.println("Replicated:             " + replicatedCache.size());
            System.out.println("No-backup (0 backups):  " + noBackupCache.size());

            System.out.println("\n=== Node 1 Local Sizes ===");
            System.out.println("Partitioned: primary=" + partitionedCache.localSize(CachePeekMode.PRIMARY) +
                ", backup=" + partitionedCache.localSize(CachePeekMode.BACKUP));
            System.out.println("Replicated:  " + replicatedCache.localSize(CachePeekMode.ALL) + " (full copy)");
            System.out.println("No-backup:   primary=" + noBackupCache.localSize(CachePeekMode.PRIMARY));

            System.out.println("\n=== Node 2 Local Sizes ===");
            System.out.println("Partitioned: primary=" + partitionedNode2.localSize(CachePeekMode.PRIMARY) +
                ", backup=" + partitionedNode2.localSize(CachePeekMode.BACKUP));
            System.out.println("Replicated:  " + replicatedNode2.localSize(CachePeekMode.ALL) + " (full copy)");
            System.out.println("No-backup:   primary=" + noBackupNode2.localSize(CachePeekMode.PRIMARY));

            System.out.println("\n=== Node 3 Local Sizes ===");
            System.out.println("Partitioned: primary=" + partitionedNode3.localSize(CachePeekMode.PRIMARY) +
                ", backup=" + partitionedNode3.localSize(CachePeekMode.BACKUP));
            System.out.println("Replicated:  " + replicatedNode3.localSize(CachePeekMode.ALL) + " (full copy)");
            System.out.println("No-backup:   primary=" + noBackupNode3.localSize(CachePeekMode.PRIMARY));

            System.out.println("\n=== Data Access Test ===");
            System.out.println("partitionedCache.get(1) = " + partitionedCache.get(1));
            System.out.println("replicatedCache.get(5)  = " + replicatedCache.get(5));
            System.out.println("noBackupCache.get(10)   = " + noBackupCache.get(10));

            System.out.println("\n=== Cache Mode Characteristics ===");
            System.out.println("PARTITIONED (1 backup): Data distributed, each entry on 2 nodes");
            System.out.println("REPLICATED: Full copy on every node (10 entries each)");
            System.out.println("NO-BACKUP: Data distributed across nodes, no redundancy");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            node3.close();
            node2.close();
            node1.close();
        }
    }

    private static Ignite startNode(int nodeNumber) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-mode-node-" + nodeNumber);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);
        discoverySpi.setIpFinder(sharedIpFinder);
        cfg.setDiscoverySpi(discoverySpi);

        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        cfg.setCommunicationSpi(commSpi);

        return Ignition.start(cfg);
    }
}
