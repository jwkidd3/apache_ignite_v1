package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Near Cache Server
 *
 * Starts a server node and creates a cache for demonstrating near cache.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.NearCacheServer"
 */
public class NearCacheServer {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
    }

    public static void main(String[] args) {
        // Start server node
        Ignite ignite = startNode();

        try {
            // Create a PARTITIONED cache for near cache demo
            CacheConfiguration<Integer, String> cacheCfg = new CacheConfiguration<>("dataCache");
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);
            cacheCfg.setBackups(1);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            // Populate cache with data
            System.out.println("\n=== Near Cache Server ===\n");
            System.out.println("Populating cache with 100 entries...");

            for (int i = 1; i <= 100; i++) {
                cache.put(i, "Value-" + i);
            }

            System.out.println("Cache size: " + cache.size());
            System.out.println("\nServer running. Press Ctrl+C to stop...");
            System.out.println("\nRun the client to see near cache in action:");
            System.out.println("  mvn exec:java -Dexec.mainClass=\"com.example.ignite.NearCacheClient\"");

            Thread.currentThread().join();

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            ignite.close();
        }
    }

    private static Ignite startNode() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("near-cache-server");

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
