package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CachePeekMode;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Cache Modes Client (Thick Client)
 *
 * Joins the cluster as a client node and reads data from caches.
 *
 * Start CacheModesServer first, then run this client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.CacheModesClient"
 */
public class CacheModesClient {

    public static void main(String[] args) {
        System.out.println("Configuring Ignite client node...");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cache-client");
        cfg.setClientMode(true);

        // Configure static IP discovery - same as server
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");

        TcpDiscoveryVmIpFinder ipFinder = new TcpDiscoveryVmIpFinder(true);
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        // Configure communication to use localhost
        TcpCommunicationSpi commSpi = new TcpCommunicationSpi();
        commSpi.setLocalAddress("127.0.0.1");
        commSpi.setLocalPort(47100);
        commSpi.setLocalPortRange(10);
        cfg.setCommunicationSpi(commSpi);

        System.out.println("Connecting to cluster...");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== Cache Modes Client (Thick Client) ===\n");
            System.out.println("Connected to cluster with " +
                ignite.cluster().forServers().nodes().size() + " server node(s)\n");

            // Get existing caches
            IgniteCache<Integer, String> partitionedCache = ignite.cache("partitionedCache");
            IgniteCache<Integer, String> replicatedCache = ignite.cache("replicatedCache");
            IgniteCache<Integer, String> noBackupCache = ignite.cache("noBackupCache");

            if (partitionedCache == null) {
                System.out.println("ERROR: Caches not found. Start CacheModesServer first!");
                return;
            }

            // Show cache sizes
            System.out.println("=== Cache Sizes ===");
            System.out.println("Partitioned (1 backup): " + partitionedCache.size(CachePeekMode.ALL));
            System.out.println("Replicated:             " + replicatedCache.size(CachePeekMode.ALL));
            System.out.println("No-backup (0 backups):  " + noBackupCache.size(CachePeekMode.ALL));

            // Read some data
            System.out.println("\n=== Data Access ===");
            System.out.println("partitionedCache.get(1)  = " + partitionedCache.get(1));
            System.out.println("partitionedCache.get(5)  = " + partitionedCache.get(5));
            System.out.println("replicatedCache.get(1)   = " + replicatedCache.get(1));
            System.out.println("replicatedCache.get(10)  = " + replicatedCache.get(10));
            System.out.println("noBackupCache.get(1)     = " + noBackupCache.get(1));
            System.out.println("noBackupCache.get(7)     = " + noBackupCache.get(7));

            // Explain cache mode behavior
            System.out.println("\n=== Cache Mode Characteristics ===");
            System.out.println("PARTITIONED (1 backup):");
            System.out.println("  - Data distributed across nodes");
            System.out.println("  - Each entry stored on 1 primary + 1 backup node");

            System.out.println("\nREPLICATED:");
            System.out.println("  - Full copy of ALL data on EVERY node");
            System.out.println("  - Reads are always local (fast)");

            System.out.println("\nPARTITIONED (0 backups):");
            System.out.println("  - Data distributed, NO redundancy");
            System.out.println("  - If a node fails, its data is LOST");

            System.out.println("\n=== Client Complete ===\n");

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
