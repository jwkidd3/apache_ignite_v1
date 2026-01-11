package com.example.ignite.solutions.lab10;

import org.apache.ignite.Ignite;
import org.apache.ignite.Ignition;
import org.apache.ignite.client.ClientCache;
import org.apache.ignite.client.ClientException;
import org.apache.ignite.client.IgniteClient;
import org.apache.ignite.configuration.ClientConfiguration;
import org.apache.ignite.configuration.ClientConnectorConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 10 Exercise 2: Thin Client Implementation
 *
 * Demonstrates:
 * - Basic thin client connection
 * - Connection configuration
 * - Thin client cache operations
 * - Performance comparison
 */
public class Lab10ThinClient {

    private static volatile boolean serverRunning = true;

    public static void main(String[] args) {
        System.out.println("=== Thin Client Implementation Lab ===\n");

        // Start server node first
        Thread serverThread = startServerNode();

        // Wait for server to be ready
        try {
            Thread.sleep(3000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            // Demonstrate thin client features
            demonstrateBasicConnection();
            demonstrateConnectionPooling();
            demonstrateConcurrentAccess();
            demonstrateBatchOperations();

            System.out.println("\n=== Thin Client Summary ===");
            System.out.println("Benefits:");
            System.out.println("  - Lightweight: No cluster participation");
            System.out.println("  - Cross-language: Available for Java, .NET, C++, Python, Node.js");
            System.out.println("  - Easy deployment: No discovery configuration needed");
            System.out.println("  - Low memory: Minimal resource footprint");
            System.out.println("");
            System.out.println("Limitations:");
            System.out.println("  - No compute grid access (limited)");
            System.out.println("  - No transactions (without additional config)");
            System.out.println("  - Network hop for every operation");

            System.out.println("\nPress Enter to stop server...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            serverRunning = false;
            serverThread.interrupt();
        }
    }

    private static Thread startServerNode() {
        Thread serverThread = new Thread(() -> {
            IgniteConfiguration cfg = new IgniteConfiguration();
            cfg.setIgniteInstanceName("thin-client-server");

            // Configure client connector for thin clients
            ClientConnectorConfiguration clientCfg = new ClientConnectorConfiguration();
            clientCfg.setPort(10800);
            clientCfg.setPortRange(10);  // Try ports 10800-10810
            clientCfg.setThreadPoolSize(8);
            clientCfg.setIdleTimeout(30000);
            cfg.setClientConnectorConfiguration(clientCfg);

            try (Ignite ignite = Ignition.start(cfg)) {
                System.out.println("Server node started, thin client port: 10800\n");

                // Keep server running
                while (serverRunning) {
                    Thread.sleep(1000);
                }
            } catch (InterruptedException e) {
                // Normal shutdown
            } catch (Exception e) {
                e.printStackTrace();
            }
        });
        serverThread.start();
        return serverThread;
    }

    private static void demonstrateBasicConnection() {
        System.out.println("=== 1. Basic Thin Client Connection ===\n");

        // Configure thin client
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800");

        try (IgniteClient client = Ignition.startClient(cfg)) {
            System.out.println("Connected to cluster via thin client");

            // Create and use cache
            ClientCache<Integer, String> cache = client.getOrCreateCache("thinClientCache");

            // Put data
            cache.put(1, "Hello");
            cache.put(2, "Thin");
            cache.put(3, "Client");

            // Get data
            System.out.println("Retrieved: " + cache.get(1) + " " + cache.get(2) + " " + cache.get(3));

            // Cache size
            System.out.println("Cache size: " + cache.size());

            // PutAll
            Map<Integer, String> batch = new HashMap<>();
            batch.put(4, "Batch1");
            batch.put(5, "Batch2");
            batch.put(6, "Batch3");
            cache.putAll(batch);
            System.out.println("After batch put, size: " + cache.size());

            // GetAll
            Map<Integer, String> results = cache.getAll(
                new HashSet<>(Arrays.asList(4, 5, 6)));
            System.out.println("GetAll results: " + results);

            // Replace operation
            boolean replaced = cache.replace(1, "Hello", "Hello World");
            System.out.println("Replace succeeded: " + replaced);
            System.out.println("New value: " + cache.get(1));

            // Remove operation
            cache.remove(6);
            System.out.println("After remove, size: " + cache.size());

            System.out.println();

        } catch (ClientException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void demonstrateConnectionPooling() {
        System.out.println("=== 2. Connection Configuration ===\n");

        // Advanced client configuration
        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800")
            .setTcpNoDelay(true)
            .setSendBufferSize(65536)
            .setReceiveBufferSize(65536)
            .setTimeout(5000)                    // Operation timeout
            .setReconnectThrottlingPeriod(30000) // Reconnect throttling
            .setReconnectThrottlingRetries(3)    // Max retries in throttling period
            .setPartitionAwarenessEnabled(true);  // Route to primary node

        System.out.println("Connection Configuration:");
        System.out.println("  - TCP NoDelay: enabled");
        System.out.println("  - Send/Receive buffer: 64KB");
        System.out.println("  - Operation timeout: 5 seconds");
        System.out.println("  - Affinity awareness: enabled");
        System.out.println("  - Reconnect throttling: 3 retries per 30 seconds");

        try (IgniteClient client = Ignition.startClient(cfg)) {
            ClientCache<String, String> cache = client.getOrCreateCache("configuredCache");
            cache.put("test", "value");
            System.out.println("\nConnection successful with advanced configuration");
            System.out.println();
        } catch (ClientException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }

    private static void demonstrateConcurrentAccess() {
        System.out.println("=== 3. Concurrent Access ===\n");

        int numThreads = 5;
        int operationsPerThread = 100;
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        CountDownLatch latch = new CountDownLatch(numThreads);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger errorCount = new AtomicInteger(0);

        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800");

        long startTime = System.currentTimeMillis();

        for (int t = 0; t < numThreads; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try (IgniteClient client = Ignition.startClient(cfg)) {
                    ClientCache<String, Integer> cache =
                        client.getOrCreateCache("concurrentCache");

                    for (int i = 0; i < operationsPerThread; i++) {
                        String key = "thread-" + threadId + "-key-" + i;
                        cache.put(key, i);
                        Integer value = cache.get(key);
                        if (value != null && value == i) {
                            successCount.incrementAndGet();
                        } else {
                            errorCount.incrementAndGet();
                        }
                    }
                } catch (Exception e) {
                    errorCount.addAndGet(operationsPerThread);
                } finally {
                    latch.countDown();
                }
            });
        }

        try {
            latch.await(30, TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        long duration = System.currentTimeMillis() - startTime;
        int totalOps = numThreads * operationsPerThread * 2; // put + get

        System.out.println("Concurrent access test results:");
        System.out.println("  Threads: " + numThreads);
        System.out.println("  Operations per thread: " + operationsPerThread);
        System.out.println("  Total operations: " + totalOps);
        System.out.println("  Successful: " + successCount.get());
        System.out.println("  Errors: " + errorCount.get());
        System.out.println("  Duration: " + duration + "ms");
        System.out.println("  Throughput: " + (totalOps * 1000 / duration) + " ops/sec");
        System.out.println();

        executor.shutdown();
    }

    private static void demonstrateBatchOperations() {
        System.out.println("=== 4. Batch Operations ===\n");

        ClientConfiguration cfg = new ClientConfiguration()
            .setAddresses("127.0.0.1:10800");

        try (IgniteClient client = Ignition.startClient(cfg)) {
            ClientCache<Integer, String> cache = client.getOrCreateCache("batchCache");

            // Prepare batch data
            Map<Integer, String> batchData = new HashMap<>();
            for (int i = 0; i < 1000; i++) {
                batchData.put(i, "Value-" + i);
            }

            // Single put benchmark
            long singleStart = System.currentTimeMillis();
            for (Map.Entry<Integer, String> entry : batchData.entrySet()) {
                cache.put(entry.getKey(), entry.getValue());
            }
            long singleDuration = System.currentTimeMillis() - singleStart;

            // Clear for next test
            cache.clear();

            // Batch put benchmark
            long batchStart = System.currentTimeMillis();
            cache.putAll(batchData);
            long batchDuration = System.currentTimeMillis() - batchStart;

            System.out.println("Batch vs Single Put Comparison (1000 entries):");
            System.out.println("  Single puts:  " + singleDuration + "ms");
            System.out.println("  Batch putAll: " + batchDuration + "ms");
            if (batchDuration > 0) {
                System.out.println("  Speedup:      " +
                    String.format("%.2fx", (double) singleDuration / batchDuration));
            }
            System.out.println();

        } catch (ClientException e) {
            System.err.println("Client error: " + e.getMessage());
        }
    }
}
