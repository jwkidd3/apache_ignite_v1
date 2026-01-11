package com.example.ignite.solutions.lab04;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;

/**
 * Lab 4 Exercise 2: Load Configuration from XML
 *
 * This exercise demonstrates loading Ignite configuration
 * from an XML file.
 */
public class XmlConfig {

    public static void main(String[] args) {
        System.out.println("=== XML Configuration Lab ===\n");

        // Load configuration from XML file
        try (Ignite ignite = Ignition.start("ignite-config.xml")) {
            System.out.println("Node started with XML configuration");
            System.out.println("Node name: " + ignite.name());

            // Access pre-configured caches
            IgniteCache<Integer, String> partitionedCache =
                ignite.cache("xmlPartitionedCache");
            IgniteCache<Integer, String> replicatedCache =
                ignite.cache("xmlReplicatedCache");

            System.out.println("\n=== Available Caches ===");
            ignite.cacheNames().forEach(name ->
                System.out.println("  - " + name));

            // Test the caches
            if (partitionedCache != null) {
                partitionedCache.put(1, "XML Configured Value");
                System.out.println("\nTest value: " +
                    partitionedCache.get(1));
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            System.err.println("Error loading XML configuration: " + e.getMessage());
            System.err.println("Make sure ignite-config.xml is in the classpath/resources folder");
            e.printStackTrace();
        }
    }
}
