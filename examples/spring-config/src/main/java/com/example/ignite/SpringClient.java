package com.example.ignite;

import org.springframework.context.annotation.AnnotationConfigApplicationContext;

/**
 * Client using Spring dependency injection with Ignite.
 *
 * This demonstrates the Spring-native approach where:
 * - Ignite instance is a Spring-managed bean
 * - Services use constructor injection to get Ignite
 * - Spring manages the full lifecycle (startup and shutdown)
 *
 * Start XmlConfigServer or JavaConfigServer first, then run this client.
 *
 * Usage:
 *   mvn exec:java -Dexec.mainClass="com.example.ignite.SpringClient"
 */
public class SpringClient {

    public static void main(String[] args) {
        // Create Spring context - this starts Ignite automatically
        try (AnnotationConfigApplicationContext ctx =
                 new AnnotationConfigApplicationContext()) {

            // Register the configuration and service classes
            ctx.register(SpringClientConfig.class, CacheService.class);
            ctx.refresh();

            System.out.println("\n=== Spring Client ===\n");

            // Get the service - Ignite is already injected
            CacheService cacheService = ctx.getBean(CacheService.class);

            System.out.println("Client connected: " + cacheService.getNodeName());
            System.out.println("Server nodes: " + cacheService.getServerCount());

            // Check if caches exist
            int productsSize = cacheService.getCacheSize("products");
            if (productsSize < 0) {
                System.out.println("ERROR: Caches not found. Start server first!");
                return;
            }

            // Read data using the service
            System.out.println("\n=== Reading Data ===");
            System.out.println("Products cache size: " + productsSize);
            System.out.println("Categories cache size: " + cacheService.getCacheSize("categories"));

            cacheService.printCacheContents("products", 5);
            cacheService.printCacheContents("categories", 5);

            System.out.println("\n=== Spring Client Complete ===");

            // Context close will automatically stop Ignite via destroyMethod
        }
    }
}
