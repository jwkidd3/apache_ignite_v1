package com.example.ignite.solutions.lab08;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.expiry.CreatedExpiryPolicy;
import javax.cache.expiry.Duration;
import javax.cache.expiry.ModifiedExpiryPolicy;
import javax.cache.expiry.TouchedExpiryPolicy;
import java.util.concurrent.TimeUnit;

/**
 * Lab 08 Exercise 2: Expiry Policies
 *
 * Demonstrates:
 * - CreatedExpiryPolicy: Expires after creation time
 * - ModifiedExpiryPolicy: Expires after last modification
 * - TouchedExpiryPolicy: Expires after last access
 * - Per-entry expiry policies
 */
public class Lab08ExpiryPolicies {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Expiry Policies Lab ===\n");

            // Cache with Created Expiry Policy
            System.out.println("=== 1. Created Expiry Policy ===");
            System.out.println("Entry expires 5 seconds after creation\n");

            CacheConfiguration<Integer, String> createdCfg =
                new CacheConfiguration<>("createdExpiryCache");
            createdCfg.setCacheMode(CacheMode.PARTITIONED);
            createdCfg.setExpiryPolicyFactory(
                CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 5)));

            IgniteCache<Integer, String> createdCache =
                ignite.getOrCreateCache(createdCfg);

            createdCache.put(1, "Will expire in 5 seconds");
            System.out.println("Entry created: " + createdCache.get(1));
            System.out.println("Waiting 6 seconds...");
            Thread.sleep(6000);
            System.out.println("After expiry: " + createdCache.get(1) + " (null expected)\n");

            // Cache with Modified Expiry Policy
            System.out.println("=== 2. Modified Expiry Policy ===");
            System.out.println("Entry expires 3 seconds after last update\n");

            CacheConfiguration<Integer, String> modifiedCfg =
                new CacheConfiguration<>("modifiedExpiryCache");
            modifiedCfg.setCacheMode(CacheMode.PARTITIONED);
            modifiedCfg.setExpiryPolicyFactory(
                ModifiedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 3)));

            IgniteCache<Integer, String> modifiedCache =
                ignite.getOrCreateCache(modifiedCfg);

            modifiedCache.put(1, "Initial value");
            System.out.println("Entry created");
            Thread.sleep(2000);

            modifiedCache.put(1, "Updated value");
            System.out.println("Entry updated (timer reset)");
            Thread.sleep(2000);

            System.out.println("After 2 seconds: " + modifiedCache.get(1));
            Thread.sleep(2000);

            System.out.println("After 4 seconds: " + modifiedCache.get(1) + " (null expected)\n");

            // Cache with Touched Expiry Policy
            System.out.println("=== 3. Touched Expiry Policy ===");
            System.out.println("Entry expires 4 seconds after last access\n");

            CacheConfiguration<Integer, String> touchedCfg =
                new CacheConfiguration<>("touchedExpiryCache");
            touchedCfg.setCacheMode(CacheMode.PARTITIONED);
            touchedCfg.setExpiryPolicyFactory(
                TouchedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 4)));

            IgniteCache<Integer, String> touchedCache =
                ignite.getOrCreateCache(touchedCfg);

            touchedCache.put(1, "Touched expiry value");
            System.out.println("Entry created");

            for (int i = 0; i < 3; i++) {
                Thread.sleep(2000);
                String value = touchedCache.get(1);
                System.out.println("After " + ((i+1)*2) + " seconds: " + value + " (read resets timer)");
            }

            Thread.sleep(5000);
            System.out.println("After no access for 5 seconds: " +
                touchedCache.get(1) + " (null expected)\n");

            // Per-entry expiry policy
            System.out.println("=== 4. Per-Entry Expiry Policy ===");
            IgniteCache<Integer, String> dynamicCache =
                ignite.getOrCreateCache("dynamicExpiryCache");

            IgniteCache<Integer, String> cache2Sec =
                dynamicCache.withExpiryPolicy(
                    CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 2)).create());

            IgniteCache<Integer, String> cache5Sec =
                dynamicCache.withExpiryPolicy(
                    CreatedExpiryPolicy.factoryOf(new Duration(TimeUnit.SECONDS, 5)).create());

            cache2Sec.put(1, "Expires in 2 seconds");
            cache5Sec.put(2, "Expires in 5 seconds");

            Thread.sleep(3000);

            System.out.println("Entry 1 (2sec TTL): " + dynamicCache.get(1) + " (null expected)");
            System.out.println("Entry 2 (5sec TTL): " + dynamicCache.get(2) + " (still there)");

            System.out.println("\n=== Expiry Policy Use Cases ===");
            System.out.println("- CreatedExpiryPolicy: Session data, temporary tokens");
            System.out.println("- ModifiedExpiryPolicy: Frequently updated data");
            System.out.println("- TouchedExpiryPolicy: Recently accessed data (LRU-like)");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
