package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;
import org.apache.ignite.events.CacheEvent;
import org.apache.ignite.events.EventType;
import org.apache.ignite.lang.IgnitePredicate;

/**
 * Lab 08 Exercise 5: Cache Events
 *
 * Demonstrates:
 * - Enabling cache events
 * - Registering event listeners
 * - Handling PUT, READ, and REMOVE events
 * - Event-driven cache monitoring
 */
public class Lab08CacheEvents {

    public static void main(String[] args) {
        // Configure Ignite with events enabled
        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIncludeEventTypes(
            EventType.EVT_CACHE_OBJECT_PUT,
            EventType.EVT_CACHE_OBJECT_REMOVED,
            EventType.EVT_CACHE_OBJECT_READ
        );

        try (Ignite ignite = Ignition.start(cfg)) {
            System.out.println("=== Cache Events Lab ===\n");

            // Register event listener
            IgnitePredicate<CacheEvent> listener = evt -> {
                System.out.println("\n[EVENT] Type: " + evt.name());
                System.out.println("  Cache: " + evt.cacheName());
                System.out.println("  Key: " + evt.key());
                System.out.println("  Old Value: " + evt.oldValue());
                System.out.println("  New Value: " + evt.newValue());
                return true;  // Continue listening
            };

            ignite.events().localListen(listener,
                EventType.EVT_CACHE_OBJECT_PUT,
                EventType.EVT_CACHE_OBJECT_REMOVED,
                EventType.EVT_CACHE_OBJECT_READ);

            System.out.println("Event listener registered\n");

            // Create cache and perform operations
            CacheConfiguration<Integer, String> cacheCfg =
                new CacheConfiguration<>("eventCache");
            cacheCfg.setCacheMode(CacheMode.PARTITIONED);

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cacheCfg);

            System.out.println("=== Performing Cache Operations ===");
            System.out.println("(Watch for events below)\n");

            // PUT event
            System.out.println("1. Putting value...");
            cache.put(1, "Hello");
            Thread.sleep(100);

            // READ event
            System.out.println("\n2. Reading value...");
            cache.get(1);
            Thread.sleep(100);

            // UPDATE (PUT) event
            System.out.println("\n3. Updating value...");
            cache.put(1, "Hello Updated");
            Thread.sleep(100);

            // REMOVE event
            System.out.println("\n4. Removing value...");
            cache.remove(1);
            Thread.sleep(100);

            // Batch operations
            System.out.println("\n5. Batch put operation...");
            for (int i = 10; i < 15; i++) {
                cache.put(i, "Batch-" + i);
            }
            Thread.sleep(200);

            System.out.println("\n=== Event Use Cases ===");
            System.out.println("- Audit logging");
            System.out.println("- Cache statistics");
            System.out.println("- Triggering workflows");
            System.out.println("- Replication to other systems");
            System.out.println("- Monitoring and alerting");

            System.out.println("\n=== Event Filtering ===");
            System.out.println("- Filter by event type");
            System.out.println("- Filter by cache name");
            System.out.println("- Filter by key pattern");
            System.out.println("- Custom predicate logic");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

            // Cleanup
            ignite.events().stopLocalListen(listener);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
