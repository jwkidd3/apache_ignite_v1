package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.configuration.NearCacheConfiguration;
import org.apache.ignite.cache.eviction.lru.LruEvictionPolicyFactory;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;

/**
 * Near Cache Client
 *
 * Demonstrates the performance benefits of near cache (client-side caching).
 * Near cache stores frequently accessed data locally on the client,
 * reducing network round-trips for repeated reads.
 *
 * Start NearCacheServer first, then run this client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.NearCacheClient"
 */
public class NearCacheClient {

    private static TcpDiscoveryVmIpFinder sharedIpFinder = new TcpDiscoveryVmIpFinder(true);

    static {
        sharedIpFinder.setAddresses(Arrays.asList("127.0.0.1:47500", "127.0.0.1:47501", "127.0.0.1:47502"));
    }

    public static void main(String[] args) {
        // Start two clients to compare: one with near cache, one without
        Ignite clientWithNear = startClient("near-cache-client");
        Ignite clientWithoutNear = startClient("regular-client");

        try {
            System.out.println("\n=== Near Cache Client Demo ===\n");
            System.out.println("Client with near cache: " + clientWithNear.name());
            System.out.println("Client without near cache: " + clientWithoutNear.name());
            System.out.println("Server nodes: " + clientWithNear.cluster().forServers().nodes().size());

            // Configure near cache with LRU eviction (max 50 entries)
            NearCacheConfiguration<Integer, String> nearCfg = new NearCacheConfiguration<>();
            nearCfg.setNearEvictionPolicyFactory(new LruEvictionPolicyFactory<>(50));

            // Get cache WITH near cache on first client
            IgniteCache<Integer, String> nearCache =
                clientWithNear.getOrCreateNearCache("dataCache", nearCfg);

            // Get cache WITHOUT near cache on second client
            IgniteCache<Integer, String> remoteCache = clientWithoutNear.cache("dataCache");

            if (nearCache == null || remoteCache == null) {
                System.out.println("ERROR: Cache not found. Start server first!");
                return;
            }

            // Warm up both caches
            nearCache.get(1);
            remoteCache.get(1);

            System.out.println("\n=== Performance Comparison ===\n");

            int testKey = 42;
            int iterations = 1000;

            System.out.println("Reading key " + testKey + " " + iterations + " times...\n");

            // Without near cache - every read goes to server
            long startRemote = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                remoteCache.get(testKey);
            }
            long remoteTime = System.nanoTime() - startRemote;

            // With near cache - first read fetches, subsequent reads are local
            long startNear = System.nanoTime();
            for (int i = 0; i < iterations; i++) {
                nearCache.get(testKey);
            }
            long nearTime = System.nanoTime() - startNear;

            System.out.println("Without near cache: " + (remoteTime / 1_000_000) + " ms");
            System.out.println("With near cache:    " + (nearTime / 1_000_000) + " ms");
            System.out.println("Speedup:            " + String.format("%.1fx faster", (double) remoteTime / nearTime));

            // Show near cache population
            System.out.println("\n=== Near Cache Behavior ===\n");

            System.out.println("Reading keys 1-10 via near cache...");
            for (int i = 1; i <= 10; i++) {
                String value = nearCache.get(i);
                System.out.println("  Key " + i + " -> " + value);
            }

            System.out.println("\nNear cache characteristics:");
            System.out.println("  - Stores recently accessed entries on client");
            System.out.println("  - Reduces network round-trips for repeated reads");
            System.out.println("  - Automatically invalidated when data changes");
            System.out.println("  - Configurable eviction policy (LRU with max 50 entries)");

            // Show invalidation
            System.out.println("\n=== Cache Invalidation Demo ===\n");

            String originalValue = nearCache.get(1);
            System.out.println("Original value for key 1: " + originalValue);

            // Update via the cache (will invalidate near cache entry)
            nearCache.put(1, "Updated-Value-1");
            System.out.println("Updated key 1 to: Updated-Value-1");

            String newValue = nearCache.get(1);
            System.out.println("Value after update: " + newValue);
            System.out.println("Near cache correctly reflects the update!");

            // Show that the other client also sees the update
            String otherClientValue = remoteCache.get(1);
            System.out.println("Other client sees: " + otherClientValue);

            System.out.println("\n=== Near Cache Client Complete ===");

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            clientWithoutNear.close();
            clientWithNear.close();
        }
    }

    private static Ignite startClient(String name) {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName(name);
        cfg.setClientMode(true);

        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
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
