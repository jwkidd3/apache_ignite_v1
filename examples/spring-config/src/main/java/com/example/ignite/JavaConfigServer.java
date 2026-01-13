package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Server using Java-based Spring configuration
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.JavaConfigServer"
 */
public class JavaConfigServer {

    public static void main(String[] args) {
        // Create Spring context from Java config
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext(IgniteServerConfig.class)) {

            // Get IgniteConfiguration bean
            IgniteConfiguration cfg = ctx.getBean(IgniteConfiguration.class);

            // Start Ignite with the configuration
            try (Ignite ignite = Ignition.start(cfg)) {
                System.out.println("\n=== Java Config Server ===\n");
                System.out.println("Server started: " + ignite.name());
                System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

                // Get caches defined in Java config
                IgniteCache<Integer, String> products = ignite.cache("products");
                IgniteCache<Integer, String> categories = ignite.cache("categories");

                // Populate caches
                System.out.println("\nPopulating caches...");
                for (int i = 1; i <= 5; i++) {
                    products.put(i, "Product-" + i);
                    categories.put(i, "Category-" + i);
                }

                System.out.println("Products cache size: " + products.size());
                System.out.println("Categories cache size: " + categories.size());

                System.out.println("\nServer running. Press Ctrl+C to stop...");
                Thread.currentThread().join();

            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
