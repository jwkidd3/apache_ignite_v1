package com.example.ignite.solutions.lab10;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.HashMap;
import java.util.Map;

/**
 * Lab 10 Exercise 3: Spring Integration Patterns
 *
 * This demonstrates patterns for Spring integration.
 * For a full Spring Boot application, use Spring Boot's auto-configuration.
 *
 * Demonstrates:
 * - Service layer patterns
 * - Cache abstraction
 * - Configuration patterns
 */
public class Lab10SpringIntegration {

    public static void main(String[] args) {
        System.out.println("=== Spring Integration Patterns Lab ===\n");

        try (Ignite ignite = Ignition.start()) {
            System.out.println("Ignite node started\n");

            // Create service instance (simulating Spring injection)
            CacheService cacheService = new CacheService(ignite);
            UserService userService = new UserService(cacheService);

            // Test cache service
            System.out.println("=== Testing Cache Service ===");
            cacheService.put("myCache", "key1", "value1");
            System.out.println("Put: key1 -> value1");

            Object value = cacheService.get("myCache", "key1");
            System.out.println("Get key1: " + value);

            System.out.println("Cache size: " + cacheService.size("myCache"));

            // Test user service with caching
            System.out.println("\n=== Testing User Service ===");

            // First call - will simulate database fetch
            long start = System.currentTimeMillis();
            String user1 = userService.getUser(101);
            long time1 = System.currentTimeMillis() - start;
            System.out.println("First call (DB): " + user1 + " - " + time1 + "ms");

            // Second call - from cache
            start = System.currentTimeMillis();
            String user1Cached = userService.getUser(101);
            long time2 = System.currentTimeMillis() - start;
            System.out.println("Second call (Cache): " + user1Cached + " - " + time2 + "ms");

            // Batch operations
            System.out.println("\n=== Batch Operations ===");
            Map<Integer, String> users = new HashMap<>();
            users.put(201, "Alice");
            users.put(202, "Bob");
            users.put(203, "Charlie");
            userService.saveUsers(users);
            System.out.println("Saved " + users.size() + " users");

            // Retrieve batch
            Map<Integer, String> retrieved = userService.getUsers(201, 202, 203);
            System.out.println("Retrieved: " + retrieved);

            System.out.println("\n=== Spring Integration Patterns ===");
            System.out.println("1. @Cacheable - Cache method results automatically");
            System.out.println("2. @CacheEvict - Remove entries on updates");
            System.out.println("3. @CachePut - Always update cache");
            System.out.println("4. CacheManager - Manage multiple caches");
            System.out.println("5. Spring Data - Repository abstractions");

            System.out.println("\n=== Spring Boot Configuration ===");
            System.out.println("application.properties:");
            System.out.println("  spring.cache.type=ignite");
            System.out.println("  ignite.cache.name=myCache");
            System.out.println("  ignite.config.path=classpath:ignite-config.xml");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Simulates a Spring @Service for cache operations
     */
    static class CacheService {
        private final Ignite ignite;

        public CacheService(Ignite ignite) {
            this.ignite = ignite;
        }

        public void put(String cacheName, Object key, Object value) {
            IgniteCache<Object, Object> cache = getOrCreateCache(cacheName);
            cache.put(key, value);
        }

        public Object get(String cacheName, Object key) {
            IgniteCache<Object, Object> cache = ignite.cache(cacheName);
            return cache != null ? cache.get(key) : null;
        }

        public int size(String cacheName) {
            IgniteCache<Object, Object> cache = ignite.cache(cacheName);
            return cache != null ? cache.size() : 0;
        }

        public void evict(String cacheName, Object key) {
            IgniteCache<Object, Object> cache = ignite.cache(cacheName);
            if (cache != null) {
                cache.remove(key);
            }
        }

        public void clear(String cacheName) {
            IgniteCache<Object, Object> cache = ignite.cache(cacheName);
            if (cache != null) {
                cache.clear();
            }
        }

        private IgniteCache<Object, Object> getOrCreateCache(String name) {
            IgniteCache<Object, Object> cache = ignite.cache(name);
            if (cache == null) {
                CacheConfiguration<Object, Object> cfg = new CacheConfiguration<>(name);
                cache = ignite.getOrCreateCache(cfg);
            }
            return cache;
        }
    }

    /**
     * Simulates a Spring @Service with caching
     */
    static class UserService {
        private final CacheService cacheService;
        private static final String USER_CACHE = "userCache";

        public UserService(CacheService cacheService) {
            this.cacheService = cacheService;
        }

        // Simulates @Cacheable behavior
        public String getUser(int userId) {
            String cacheKey = "user:" + userId;

            // Check cache first
            Object cached = cacheService.get(USER_CACHE, cacheKey);
            if (cached != null) {
                return (String) cached;
            }

            // Simulate database fetch
            String user = fetchFromDatabase(userId);

            // Store in cache
            cacheService.put(USER_CACHE, cacheKey, user);

            return user;
        }

        // Simulates @CacheEvict behavior
        public void deleteUser(int userId) {
            String cacheKey = "user:" + userId;
            cacheService.evict(USER_CACHE, cacheKey);
            // Also delete from database...
        }

        // Simulates @CachePut behavior
        public String updateUser(int userId, String name) {
            String cacheKey = "user:" + userId;
            // Update database first...

            // Update cache
            cacheService.put(USER_CACHE, cacheKey, name);
            return name;
        }

        public void saveUsers(Map<Integer, String> users) {
            for (Map.Entry<Integer, String> entry : users.entrySet()) {
                String cacheKey = "user:" + entry.getKey();
                cacheService.put(USER_CACHE, cacheKey, entry.getValue());
            }
        }

        public Map<Integer, String> getUsers(Integer... userIds) {
            Map<Integer, String> result = new HashMap<>();
            for (Integer userId : userIds) {
                String cacheKey = "user:" + userId;
                Object value = cacheService.get(USER_CACHE, cacheKey);
                if (value != null) {
                    result.put(userId, (String) value);
                }
            }
            return result;
        }

        private String fetchFromDatabase(int userId) {
            // Simulate slow database call
            try {
                Thread.sleep(500);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            return "User-" + userId;
        }
    }
}
