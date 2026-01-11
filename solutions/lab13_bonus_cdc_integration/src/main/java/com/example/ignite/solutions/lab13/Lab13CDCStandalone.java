package com.example.ignite.solutions.lab13;

import com.example.ignite.solutions.lab13.cdc.IgniteCDCConsumer;
import com.example.ignite.solutions.lab13.model.*;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import javax.cache.Cache;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * Standalone version of Lab 13 - runs Ignite embedded (no Docker Ignite needed)
 *
 * Use this version when:
 * - You only want Docker for PostgreSQL, Kafka, and Debezium
 * - You prefer to run Ignite locally in the JVM
 *
 * Prerequisites:
 * 1. Start Docker services (without Ignite):
 *    docker-compose up -d postgres zookeeper kafka debezium-connect kafka-ui
 * 2. Register Debezium connector
 * 3. Run this application
 */
public class Lab13CDCStandalone {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:29092";
    private static final List<String> CDC_TOPICS = Arrays.asList(
            "dbserver1.inventory.customers",
            "dbserver1.inventory.products",
            "dbserver1.inventory.orders",
            "dbserver1.inventory.order_items"
    );

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║   Lab 13: CDC Integration - Standalone Ignite Mode            ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║  PostgreSQL -> Debezium -> Kafka -> Consumer -> Ignite (JVM)  ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        // Configure Ignite - embedded server mode
        IgniteConfiguration cfg = createEmbeddedIgniteConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Apache Ignite started in embedded mode\n");

            // Initialize caches
            initializeCaches(ignite);

            // Create and start CDC consumer
            IgniteCDCConsumer cdcConsumer = new IgniteCDCConsumer(
                    ignite, KAFKA_BOOTSTRAP_SERVERS, CDC_TOPICS);

            ExecutorService executor = Executors.newSingleThreadExecutor();
            executor.submit(cdcConsumer);

            // Interactive menu
            runInteractiveMenu(ignite, cdcConsumer);

            // Cleanup
            System.out.println("\nShutting down...");
            cdcConsumer.stop();
            executor.shutdown();
            executor.awaitTermination(10, TimeUnit.SECONDS);

        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static IgniteConfiguration createEmbeddedIgniteConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cdc-embedded-node");
        cfg.setClientMode(false);  // Server mode - embedded
        cfg.setPeerClassLoadingEnabled(true);

        // Standalone discovery (single node)
        org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi discoverySpi =
                new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi();
        org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder ipFinder =
                new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }

    private static void initializeCaches(Ignite ignite) {
        CacheConfiguration<Integer, Customer> customerCfg = new CacheConfiguration<>("customers");
        customerCfg.setStatisticsEnabled(true);
        customerCfg.setIndexedTypes(Integer.class, Customer.class);

        CacheConfiguration<Integer, Product> productCfg = new CacheConfiguration<>("products");
        productCfg.setStatisticsEnabled(true);
        productCfg.setIndexedTypes(Integer.class, Product.class);

        CacheConfiguration<Integer, Order> orderCfg = new CacheConfiguration<>("orders");
        orderCfg.setStatisticsEnabled(true);
        orderCfg.setIndexedTypes(Integer.class, Order.class);

        CacheConfiguration<Integer, OrderItem> orderItemCfg = new CacheConfiguration<>("order_items");
        orderItemCfg.setStatisticsEnabled(true);
        orderItemCfg.setIndexedTypes(Integer.class, OrderItem.class);

        ignite.getOrCreateCache(customerCfg);
        ignite.getOrCreateCache(productCfg);
        ignite.getOrCreateCache(orderCfg);
        ignite.getOrCreateCache(orderItemCfg);

        System.out.println("Caches initialized with SQL indexing enabled\n");
    }

    private static void runInteractiveMenu(Ignite ignite, IgniteCDCConsumer consumer) {
        BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

        System.out.println("\n══════════════════════════════════════════════════════════════");
        System.out.println("CDC Consumer is running. Make changes in PostgreSQL to see");
        System.out.println("real-time updates in Ignite caches.");
        System.out.println("══════════════════════════════════════════════════════════════\n");

        printHelp();

        try {
            while (true) {
                System.out.print("\nCommand > ");
                String command = reader.readLine();

                if (command == null || command.equalsIgnoreCase("quit") ||
                        command.equalsIgnoreCase("exit") || command.equalsIgnoreCase("q")) {
                    break;
                }

                processCommand(command, ignite, consumer);
            }
        } catch (Exception e) {
            System.err.println("Error reading input: " + e.getMessage());
        }
    }

    private static void processCommand(String command, Ignite ignite, IgniteCDCConsumer consumer) {
        String[] parts = command.trim().split("\\s+");
        if (parts.length == 0 || parts[0].isEmpty()) return;

        String cmd = parts[0].toLowerCase();

        switch (cmd) {
            case "help":
            case "h":
                printHelp();
                break;

            case "stats":
            case "s":
                consumer.printCacheStats();
                consumer.printMetrics();
                break;

            case "customers":
            case "c":
                listCache(ignite, "customers");
                break;

            case "products":
            case "p":
                listCache(ignite, "products");
                break;

            case "orders":
            case "o":
                listCache(ignite, "orders");
                break;

            case "items":
            case "i":
                listCache(ignite, "order_items");
                break;

            case "sql":
                printSqlExamples();
                break;

            default:
                System.out.println("Unknown command. Type 'help' for available commands.");
        }
    }

    private static void printHelp() {
        System.out.println("\n=== Available Commands ===");
        System.out.println("  stats, s      - Show cache and CDC metrics");
        System.out.println("  customers, c  - List all customers");
        System.out.println("  products, p   - List all products");
        System.out.println("  orders, o     - List all orders");
        System.out.println("  items, i      - List all order items");
        System.out.println("  sql           - Show PostgreSQL test commands");
        System.out.println("  help, h       - Show this help");
        System.out.println("  quit, q       - Exit");
    }

    private static void printSqlExamples() {
        System.out.println("\n=== PostgreSQL Commands to Test CDC ===");
        System.out.println("Connect: docker exec -it postgres psql -U postgres inventory\n");
        System.out.println("INSERT:");
        System.out.println("  INSERT INTO inventory.customers (first_name, last_name, email, city)");
        System.out.println("  VALUES ('Test', 'User', 'test@example.com', 'Seattle');\n");
        System.out.println("UPDATE:");
        System.out.println("  UPDATE inventory.customers SET city = 'San Francisco' WHERE id = 1;\n");
        System.out.println("DELETE:");
        System.out.println("  DELETE FROM inventory.customers WHERE id = 6;");
    }

    private static void listCache(Ignite ignite, String cacheName) {
        IgniteCache<Integer, ?> cache = ignite.cache(cacheName);
        if (cache == null) {
            System.out.println("Cache not found: " + cacheName);
            return;
        }

        System.out.println("\n=== " + cacheName.toUpperCase() + " (" + cache.size() + " entries) ===");

        int count = 0;
        for (Cache.Entry<Integer, ?> entry : cache.query(new ScanQuery<>())) {
            System.out.println("  " + entry.getValue());
            count++;
            if (count >= 20) {
                System.out.println("  ... (showing first 20)");
                break;
            }
        }

        if (count == 0) {
            System.out.println("  (empty)");
        }
    }
}
