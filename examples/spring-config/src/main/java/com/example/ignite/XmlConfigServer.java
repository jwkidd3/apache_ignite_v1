package com.example.ignite;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

/**
 * Server using XML Spring configuration
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.XmlConfigServer"
 */
public class XmlConfigServer {

    public static void main(String[] args) {
        // Start Ignite from XML configuration
        try (Ignite ignite = Ignition.start("ignite-server-config.xml")) {
            System.out.println("\n=== XML Config Server ===\n");
            System.out.println("Server started: " + ignite.name());
            System.out.println("Cluster nodes: " + ignite.cluster().nodes().size());

            // Get caches defined in XML
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
