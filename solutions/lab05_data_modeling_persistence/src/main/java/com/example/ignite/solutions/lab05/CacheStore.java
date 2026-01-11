package com.example.ignite.solutions.lab05;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.util.HashMap;
import java.util.Map;

/**
 * Lab 5 Exercise 4: Read-Through/Write-Through Cache Store
 *
 * This exercise demonstrates implementing cache store patterns
 * for database integration.
 */
public class CacheStore {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Store Integration Lab ===\n");

            // Create cache with store
            CacheConfiguration<Integer, String> cfg =
                new CacheConfiguration<>("storeCache");
            cfg.setCacheMode(CacheMode.PARTITIONED);
            cfg.setReadThrough(true);
            cfg.setWriteThrough(true);
            cfg.setCacheStoreFactory(() -> new SimpleDatabaseStore());

            IgniteCache<Integer, String> cache = ignite.getOrCreateCache(cfg);

            System.out.println("=== Write-Through Test ===");
            cache.put(1, "Value 1");  // Writes to cache AND database
            cache.put(2, "Value 2");
            cache.put(3, "Value 3");
            System.out.println("Data written to cache and database");
            System.out.println("Cache size: " + cache.size());

            // Clear cache (but data remains in database)
            cache.clear();
            System.out.println("\nCache cleared (data still in database)");
            System.out.println("Cache size after clear: " + cache.size());

            System.out.println("\n=== Read-Through Test ===");
            String value1 = cache.get(1);  // Reads from database if not in cache
            System.out.println("Read value: " + value1);
            System.out.println("Data loaded from database to cache");
            System.out.println("Cache size after read: " + cache.size());

            // Read more values
            System.out.println("\nReading more values:");
            System.out.println("Key 2: " + cache.get(2));
            System.out.println("Key 3: " + cache.get(3));
            System.out.println("Key 99 (not in DB): " + cache.get(99));

            System.out.println("\n=== Cache Store Patterns ===");
            System.out.println("Read-Through: Load from DB on cache miss");
            System.out.println("Write-Through: Write to DB immediately on put");
            System.out.println("Write-Behind: Batch write to DB asynchronously");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Simple in-memory database simulation
    private static class SimpleDatabaseStore extends CacheStoreAdapter<Integer, String> {
        private static final Map<Integer, String> database = new HashMap<>();

        @Override
        public String load(Integer key) throws CacheLoaderException {
            System.out.println("  [DB] Loading key: " + key);
            return database.get(key);
        }

        @Override
        public void write(Cache.Entry<? extends Integer, ? extends String> entry)
                throws CacheWriterException {
            System.out.println("  [DB] Writing: " + entry.getKey() +
                " = " + entry.getValue());
            database.put(entry.getKey(), entry.getValue());
        }

        @Override
        public void delete(Object key) throws CacheWriterException {
            System.out.println("  [DB] Deleting key: " + key);
            database.remove(key);
        }
    }
}
