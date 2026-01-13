package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Client using Java-based Spring configuration
 *
 * Start XmlConfigServer or JavaConfigServer first, then run this client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.JavaConfigClient"
 */
public class JavaConfigClient {

    public static void main(String[] args) {
        // Create Spring context from Java config
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(IgniteClientConfig.class)) {

            // Get IgniteConfiguration bean
            IgniteConfiguration cfg = ctx.getBean(IgniteConfiguration.class);

            // Start Ignite client with the configuration
            try (Ignite ignite = Ignition.start(cfg)) {
                System.out.println("\n=== Java Config Client ===\n");
                System.out.println("Client connected: " + ignite.name());
                System.out.println("Server nodes: " + ignite.cluster().forServers().nodes().size());

                // Get caches
                IgniteCache<Integer, String> products = ignite.cache("products");
                IgniteCache<Integer, String> categories = ignite.cache("categories");

                if (products == null) {
                    System.out.println("ERROR: Caches not found. Start server first!");
                    return;
                }

                // Read data
                System.out.println("\n=== Reading Data ===");
                System.out.println("Products cache size: " + products.size());
                System.out.println("Categories cache size: " + categories.size());

                System.out.println("\nProducts:");
                for (int i = 1; i <= 5; i++) {
                    System.out.println("  " + i + " -> " + products.get(i));
                }

                System.out.println("\nCategories:");
                for (int i = 1; i <= 5; i++) {
                    System.out.println("  " + i + " -> " + categories.get(i));
                }

                System.out.println("\n=== Client Complete ===");

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
