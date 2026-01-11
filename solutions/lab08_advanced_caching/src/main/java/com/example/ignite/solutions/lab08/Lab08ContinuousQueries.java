package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.ContinuousQuery;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import javax.cache.event.CacheEntryEvent;
import javax.cache.event.CacheEntryListenerException;
import javax.cache.event.CacheEntryUpdatedListener;

/**
 * Lab 08 Exercise 6: Continuous Queries
 *
 * Demonstrates:
 * - Real-time notifications on cache changes
 * - Server-side filtering
 * - Event-driven architecture with caches
 * - Stock price monitoring example
 */
public class Lab08ContinuousQueries {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Continuous Queries Lab ===\n");

            CacheConfiguration<Integer, Double> cfg =
                new CacheConfiguration<>("stockPrices");

            IgniteCache<Integer, Double> cache = ignite.getOrCreateCache(cfg);

            // Create continuous query for high-value stocks
            ContinuousQuery<Integer, Double> highValueQuery =
                new ContinuousQuery<>();

            highValueQuery.setLocalListener(new CacheEntryUpdatedListener<Integer, Double>() {
                @Override
                public void onUpdated(Iterable<CacheEntryEvent<? extends Integer, ? extends Double>> events)
                        throws CacheEntryListenerException {
                    for (CacheEntryEvent<? extends Integer, ? extends Double> e : events) {
                        System.out.println("[ALERT] Stock " + e.getKey() +
                            " price changed: $" + e.getOldValue() +
                            " -> $" + e.getValue());
                    }
                }
            });

            // Filter: Only notify for stocks > $100
            highValueQuery.setRemoteFilterFactory(() ->
                evt -> evt.getValue() > 100.0);

            System.out.println("Starting continuous query (stocks > $100)...\n");

            try (QueryCursor<Cache.Entry<Integer, Double>> cursor =
                     cache.query(highValueQuery)) {

                // Simulate stock price updates
                System.out.println("=== Simulating Stock Price Updates ===\n");

                System.out.println("1. Setting initial prices...");
                cache.put(1, 50.0);   // Stock 1: $50 (below threshold, no alert)
                cache.put(2, 150.0);  // Stock 2: $150 (above threshold, alert)
                Thread.sleep(500);

                System.out.println("\n2. Updating prices...");
                cache.put(1, 120.0);  // Stock 1: $50 -> $120 (crosses threshold, alert)
                Thread.sleep(500);

                cache.put(2, 160.0);  // Stock 2: $150 -> $160 (stays above, alert)
                Thread.sleep(500);

                System.out.println("\n3. Price drops...");
                cache.put(1, 80.0);   // Stock 1: $120 -> $80 (drops below, no alert)
                Thread.sleep(500);

                // Additional demonstrations
                System.out.println("\n4. New high-value stock...");
                cache.put(3, 200.0);  // Stock 3: new entry above threshold
                Thread.sleep(500);

                System.out.println("\n=== Continuous Query Benefits ===");
                System.out.println("- Real-time notifications");
                System.out.println("- Server-side filtering (reduces traffic)");
                System.out.println("- Low latency");
                System.out.println("- Event-driven architecture");
                System.out.println("- No polling required");

                System.out.println("\n=== Use Cases ===");
                System.out.println("- Real-time dashboards");
                System.out.println("- Alert systems (price thresholds)");
                System.out.println("- Cache invalidation triggers");
                System.out.println("- Data synchronization");
                System.out.println("- Streaming analytics");
                System.out.println("- IoT sensor monitoring");

                System.out.println("\n=== Filter Options ===");
                System.out.println("- RemoteFilter: Runs on server nodes");
                System.out.println("- LocalFilter: Runs on listener node");
                System.out.println("- InitialQuery: Get existing entries first");

                System.out.println("\nPress Enter to exit...");
                System.in.read();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
