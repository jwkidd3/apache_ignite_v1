package com.example.ignite.solutions.lab04.controller;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

/**
 * Lab 4 Exercise 4: REST Controller Simulation
 *
 * This class simulates REST controller operations.
 * In a real Spring Boot app, use @RestController annotation.
 */
// @RestController
// @RequestMapping("/api/cache")
public class CacheController {

    private final Ignite ignite;
    private static final String CACHE_NAME = "springCache";

    public CacheController(Ignite ignite) {
        this.ignite = ignite;
    }

    // @PostMapping("/{key}")
    public Map<String, String> put(String key, String value) {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        cache.put(key, value);

        Map<String, String> response = new HashMap<>();
        response.put("status", "success");
        response.put("key", key);
        response.put("value", value);
        return response;
    }

    // @GetMapping("/{key}")
    public Map<String, String> get(String key) {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        String value = cache.get(key);

        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("value", value);
        return response;
    }

    // @GetMapping("/size")
    public Map<String, Integer> size() {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        Map<String, Integer> response = new HashMap<>();
        response.put("size", cache.size());
        return response;
    }

    // @DeleteMapping("/{key}")
    public Map<String, String> delete(String key) {
        IgniteCache<String, String> cache = ignite.cache(CACHE_NAME);
        boolean removed = cache.remove(key);

        Map<String, String> response = new HashMap<>();
        response.put("key", key);
        response.put("removed", String.valueOf(removed));
        return response;
    }

    /**
     * Standalone test - simulates REST API interactions
     */
    public static void main(String[] args) {
        System.out.println("=== Cache Controller Simulation ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("controller-node");

        CacheConfiguration<String, String> cacheCfg =
            new CacheConfiguration<>(CACHE_NAME);
        cfg.setCacheConfiguration(cacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            CacheController controller = new CacheController(ignite);

            System.out.println("Simulating REST API operations...\n");

            // PUT operations
            System.out.println("POST /api/cache/key1 -> " + controller.put("key1", "value1"));
            System.out.println("POST /api/cache/key2 -> " + controller.put("key2", "value2"));
            System.out.println("POST /api/cache/key3 -> " + controller.put("key3", "value3"));

            // GET operations
            System.out.println("\nGET /api/cache/key1 -> " + controller.get("key1"));
            System.out.println("GET /api/cache/key2 -> " + controller.get("key2"));

            // SIZE operation
            System.out.println("\nGET /api/cache/size -> " + controller.size());

            // DELETE operation
            System.out.println("\nDELETE /api/cache/key1 -> " + controller.delete("key1"));
            System.out.println("GET /api/cache/size -> " + controller.size());

            System.out.println("\n=== REST API Equivalent Commands ===");
            System.out.println("# Put a value");
            System.out.println("curl -X POST http://localhost:8080/api/cache/mykey \\");
            System.out.println("  -H \"Content-Type: text/plain\" -d \"myvalue\"");
            System.out.println("\n# Get a value");
            System.out.println("curl http://localhost:8080/api/cache/mykey");
            System.out.println("\n# Get cache size");
            System.out.println("curl http://localhost:8080/api/cache/size");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
