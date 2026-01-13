package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.spi.communication.tcp.TcpCommunicationSpi;
import org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi;
import org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * CRUD Operations Example
 *
 * Demonstrates basic cache operations in Apache Ignite.
 *
 * Run: mvn exec:java
 */
public class CrudOperationsExample {

    public static void main(String[] args) {
        System.out.println("Configuring Ignite node...");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("crud-example");

        // Configure static IP discovery
        TcpDiscoverySpi discoverySpi = new TcpDiscoverySpi();
        discoverySpi.setLocalAddress("127.0.0.1");
        discoverySpi.setLocalPort(47500);
        discoverySpi.setLocalPortRange(10);

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

        System.out.println("Starting Ignite node...");

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("\n=== CRUD Operations Example ===\n");

            IgniteCache<Integer, String> cache =
                ignite.getOrCreateCache("myCache");

            cache.clear();

            // === CREATE ===
            System.out.println("--- CREATE Operations ---");

            cache.put(1, "Alice");
            cache.put(2, "Bob");
            System.out.println("put(1, 'Alice'), put(2, 'Bob')");

            boolean added = cache.putIfAbsent(3, "Charlie");
            System.out.println("putIfAbsent(3, 'Charlie') = " + added);

            added = cache.putIfAbsent(1, "NewAlice");
            System.out.println("putIfAbsent(1, 'NewAlice') = " + added + " (key exists)");

            Map<Integer, String> batch = new HashMap<>();
            batch.put(4, "Diana");
            batch.put(5, "Eve");
            cache.putAll(batch);
            System.out.println("putAll({4:'Diana', 5:'Eve'})");

            // === READ ===
            System.out.println("\n--- READ Operations ---");

            String value = cache.get(1);
            System.out.println("get(1) = " + value);

            Set<Integer> keys = new HashSet<>();
            keys.add(1);
            keys.add(2);
            keys.add(3);
            Map<Integer, String> values = cache.getAll(keys);
            System.out.println("getAll({1,2,3}) = " + values);

            System.out.println("containsKey(1) = " + cache.containsKey(1));
            System.out.println("containsKey(99) = " + cache.containsKey(99));

            // === UPDATE ===
            System.out.println("\n--- UPDATE Operations ---");

            cache.put(1, "Alice Updated");
            System.out.println("put(1, 'Alice Updated')");
            System.out.println("get(1) = " + cache.get(1));

            boolean replaced = cache.replace(2, "Bob Updated");
            System.out.println("replace(2, 'Bob Updated') = " + replaced);

            replaced = cache.replace(99, "Nobody");
            System.out.println("replace(99, 'Nobody') = " + replaced + " (key doesn't exist)");

            replaced = cache.replace(3, "Charlie", "Charlie Updated");
            System.out.println("replace(3, 'Charlie', 'Charlie Updated') = " + replaced);

            String oldValue = cache.getAndPut(4, "Diana Updated");
            System.out.println("getAndPut(4, 'Diana Updated') returned: " + oldValue);

            // === DELETE ===
            System.out.println("\n--- DELETE Operations ---");

            boolean removed = cache.remove(5);
            System.out.println("remove(5) = " + removed);
            System.out.println("get(5) = " + cache.get(5));

            removed = cache.remove(4, "Diana Updated");
            System.out.println("remove(4, 'Diana Updated') = " + removed);

            oldValue = cache.getAndRemove(3);
            System.out.println("getAndRemove(3) returned: " + oldValue);

            // === FINAL STATE ===
            System.out.println("\n--- Final State ---");
            System.out.println("Cache size: " + cache.size());
            cache.forEach(e -> System.out.println("  " + e.getKey() + " -> " + e.getValue()));

            cache.clear();
            System.out.println("\nclear() - Cache size: " + cache.size());

            System.out.println("\n=== Example Complete ===\n");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
