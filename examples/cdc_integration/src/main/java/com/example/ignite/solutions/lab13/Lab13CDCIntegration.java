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
 * Lab 13 Bonus: CDC Integration with Ignite, Kafka, Debezium, and PostgreSQL
 *
 * Architecture:
 * ┌──────────────┐    ┌──────────────┐    ┌─────────┐    ┌──────────────┐
 * │  PostgreSQL  │───>│   Debezium   │───>│  Kafka  │───>│ This Consumer│───>│ Ignite │
 * │  (Source DB) │    │   (CDC)      │    │(Broker) │    │              │    │(Cache) │
 * └──────────────┘    └──────────────┘    └─────────┘    └──────────────┘
 *
 * This demonstrates:
 * - Change Data Capture (CDC) pattern
 * - Real-time cache synchronization
 * - Event-driven architecture
 * - Microservices integration
 *
 * Prerequisites:
 * 1. Start Docker environment: docker-compose up -d
 * 2. Register Debezium connector (see README)
 * 3. Run this application
 */
public class Lab13CDCIntegration {

    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:29092";
    private static final List<String> CDC_TOPICS = Arrays.asList(
            "dbserver1.inventory.customers",
            "dbserver1.inventory.products",
            "dbserver1.inventory.orders",
            "dbserver1.inventory.order_items"
    );

    public static void main(String[] args) {
        System.out.println("╔════════════════════════════════════════════════════════════════╗");
        System.out.println("║     Lab 13: CDC Integration - Ignite + Kafka + Debezium       ║");
        System.out.println("╠════════════════════════════════════════════════════════════════╣");
        System.out.println("║  PostgreSQL -> Debezium -> Kafka -> Consumer -> Apache Ignite ║");
        System.out.println("╚════════════════════════════════════════════════════════════════╝\n");

        // Configure Ignite
        IgniteConfiguration cfg = createIgniteConfig();

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("Apache Ignite started successfully\n");

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

    private static IgniteConfiguration createIgniteConfig() {
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("cdc-client-node");
        cfg.setClientMode(true);  // Connect to Docker Ignite node
        cfg.setPeerClassLoadingEnabled(true);

        // Configure discovery to find Docker Ignite node
        org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi discoverySpi =
                new org.apache.ignite.spi.discovery.tcp.TcpDiscoverySpi();
        org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder ipFinder =
                new org.apache.ignite.spi.discovery.tcp.ipfinder.vm.TcpDiscoveryVmIpFinder();
        ipFinder.setAddresses(Arrays.asList("127.0.0.1:47500..47509"));
        discoverySpi.setIpFinder(ipFinder);
        cfg.setDiscoverySpi(discoverySpi);

        return cfg;
    }

    private static void initializeCaches(Ignite ignite) {
        // Create cache configurations
        CacheConfiguration<Integer, Customer> customerCfg = new CacheConfiguration<>("customers");
        customerCfg.setBackups(1);
        customerCfg.setStatisticsEnabled(true);

        CacheConfiguration<Integer, Product> productCfg = new CacheConfiguration<>("products");
        productCfg.setBackups(1);
        productCfg.setStatisticsEnabled(true);

        CacheConfiguration<Integer, Order> orderCfg = new CacheConfiguration<>("orders");
        orderCfg.setBackups(1);
        orderCfg.setStatisticsEnabled(true);

        CacheConfiguration<Integer, OrderItem> orderItemCfg = new CacheConfiguration<>("order_items");
        orderItemCfg.setBackups(1);
        orderItemCfg.setStatisticsEnabled(true);

        ignite.getOrCreateCache(customerCfg);
        ignite.getOrCreateCache(productCfg);
        ignite.getOrCreateCache(orderCfg);
        ignite.getOrCreateCache(orderItemCfg);

        System.out.println("Caches initialized: customers, products, orders, order_items\n");
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
                listCache(ignite, "customers", Customer.class);
                break;

            case "products":
            case "p":
                listCache(ignite, "products", Product.class);
                break;

            case "orders":
            case "o":
                listCache(ignite, "orders", Order.class);
                break;

            case "items":
            case "i":
                listCache(ignite, "order_items", OrderItem.class);
                break;

            case "get":
                if (parts.length >= 3) {
                    getEntry(ignite, parts[1], Integer.parseInt(parts[2]));
                } else {
                    System.out.println("Usage: get <cache> <id>");
                }
                break;

            case "clear":
                if (parts.length >= 2) {
                    clearCache(ignite, parts[1]);
                } else {
                    System.out.println("Usage: clear <cache>");
                }
                break;

            case "sql":
                printSqlExamples();
                break;

            default:
                System.out.println("Unknown command: " + cmd);
                printHelp();
        }
    }

    private static void printHelp() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║                    Available Commands                         ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  stats, s      - Show cache and CDC metrics                  ║");
        System.out.println("║  customers, c  - List all customers                          ║");
        System.out.println("║  products, p   - List all products                           ║");
        System.out.println("║  orders, o     - List all orders                             ║");
        System.out.println("║  items, i      - List all order items                        ║");
        System.out.println("║  get <cache> <id> - Get specific entry                       ║");
        System.out.println("║  clear <cache> - Clear a cache                               ║");
        System.out.println("║  sql           - Show PostgreSQL test commands               ║");
        System.out.println("║  help, h       - Show this help                              ║");
        System.out.println("║  quit, q       - Exit                                        ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    private static void printSqlExamples() {
        System.out.println("\n╔══════════════════════════════════════════════════════════════╗");
        System.out.println("║           PostgreSQL Commands to Test CDC                     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  Connect: docker exec -it postgres psql -U postgres inventory║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  INSERT:                                                     ║");
        System.out.println("║  INSERT INTO inventory.customers                             ║");
        System.out.println("║    (first_name, last_name, email, city)                      ║");
        System.out.println("║  VALUES ('Test', 'User', 'test@example.com', 'Seattle');     ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  UPDATE:                                                     ║");
        System.out.println("║  UPDATE inventory.customers                                  ║");
        System.out.println("║  SET city = 'San Francisco' WHERE id = 1;                    ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  DELETE:                                                     ║");
        System.out.println("║  DELETE FROM inventory.customers WHERE id = 6;               ║");
        System.out.println("╠══════════════════════════════════════════════════════════════╣");
        System.out.println("║  UPDATE PRODUCT QUANTITY:                                    ║");
        System.out.println("║  UPDATE inventory.products SET quantity = 999 WHERE id = 1; ║");
        System.out.println("╚══════════════════════════════════════════════════════════════╝");
    }

    private static <V> void listCache(Ignite ignite, String cacheName, Class<V> valueClass) {
        IgniteCache<Integer, V> cache = ignite.cache(cacheName);
        if (cache == null) {
            System.out.println("Cache not found: " + cacheName);
            return;
        }

        System.out.println("\n=== " + cacheName.toUpperCase() + " (" + cache.size() + " entries) ===");

        int count = 0;
        for (Cache.Entry<Integer, V> entry : cache.query(new ScanQuery<Integer, V>())) {
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

    private static void getEntry(Ignite ignite, String cacheName, Integer id) {
        IgniteCache<Integer, ?> cache = ignite.cache(cacheName);
        if (cache == null) {
            System.out.println("Cache not found: " + cacheName);
            return;
        }

        Object value = cache.get(id);
        if (value != null) {
            System.out.println(value);
        } else {
            System.out.println("Entry not found: " + cacheName + "[" + id + "]");
        }
    }

    private static void clearCache(Ignite ignite, String cacheName) {
        IgniteCache<?, ?> cache = ignite.cache(cacheName);
        if (cache == null) {
            System.out.println("Cache not found: " + cacheName);
            return;
        }

        int size = cache.size();
        cache.clear();
        System.out.println("Cleared " + size + " entries from " + cacheName);
    }
}
