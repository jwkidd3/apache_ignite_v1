package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.lang.IgniteBiPredicate;

import javax.cache.Cache;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Lab 3 Optional: Cache Iteration and ScanQuery
 *
 * This exercise demonstrates iterating over cache entries
 * using forEach and ScanQuery with filters.
 */
public class CacheIteration {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Cache Iteration and ScanQuery Lab ===\n");

            CacheConfiguration<Integer, Product> cfg =
                new CacheConfiguration<>("productsCache");
            IgniteCache<Integer, Product> cache = ignite.getOrCreateCache(cfg);

            // Populate cache with sample data
            System.out.println("1. Populating cache with sample products...");
            Map<Integer, Product> products = new HashMap<>();
            products.put(1, new Product("Laptop", "Electronics", 999.99));
            products.put(2, new Product("Phone", "Electronics", 699.99));
            products.put(3, new Product("Tablet", "Electronics", 499.99));
            products.put(4, new Product("Desk", "Furniture", 299.99));
            products.put(5, new Product("Chair", "Furniture", 149.99));
            products.put(6, new Product("Book", "Education", 29.99));
            products.put(7, new Product("Notebook", "Education", 9.99));
            products.put(8, new Product("Monitor", "Electronics", 349.99));
            products.put(9, new Product("Keyboard", "Electronics", 79.99));
            products.put(10, new Product("Mouse", "Electronics", 39.99));

            cache.putAll(products);
            System.out.println("   Added " + products.size() + " products");

            // 2. forEach iteration
            System.out.println("\n2. Using forEach to iterate cache:");
            AtomicInteger count = new AtomicInteger(0);
            cache.forEach(entry -> {
                count.incrementAndGet();
                if (count.get() <= 3) { // Show first 3 only
                    System.out.println("   " + entry.getKey() + ": " + entry.getValue());
                }
            });
            System.out.println("   ... (total " + count.get() + " entries)");

            // 3. ScanQuery - retrieve all entries
            System.out.println("\n3. ScanQuery - All Entries:");
            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>())) {
                int displayed = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    if (displayed++ < 3) {
                        System.out.println("   " + entry.getKey() + ": " + entry.getValue());
                    }
                }
                System.out.println("   ... (query completed)");
            }

            // 4. ScanQuery with filter - find electronics
            System.out.println("\n4. ScanQuery with Filter - Electronics Only:");
            IgniteBiPredicate<Integer, Product> electronicsFilter =
                (key, product) -> "Electronics".equals(product.getCategory());

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>(electronicsFilter))) {
                double totalValue = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    Product p = entry.getValue();
                    System.out.println("   " + p.getName() + " - $" + p.getPrice());
                    totalValue += p.getPrice();
                }
                System.out.println("   Total electronics value: $" + totalValue);
            }

            // 5. ScanQuery with filter - price range
            System.out.println("\n5. ScanQuery - Products under $100:");
            IgniteBiPredicate<Integer, Product> priceFilter =
                (key, product) -> product.getPrice() < 100;

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(new ScanQuery<>(priceFilter))) {
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    Product p = entry.getValue();
                    System.out.println("   " + p.getName() + " - $" + p.getPrice());
                }
            }

            // 6. ScanQuery with local-only flag
            System.out.println("\n6. ScanQuery - Local Entries Only:");
            ScanQuery<Integer, Product> localQuery = new ScanQuery<>();
            localQuery.setLocal(true);

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(localQuery)) {
                int localCount = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    localCount++;
                }
                System.out.println("   Found " + localCount + " entries on local node");
            }

            // 7. ScanQuery with page size
            System.out.println("\n7. ScanQuery with Page Size (pagination):");
            ScanQuery<Integer, Product> pagedQuery = new ScanQuery<>();
            pagedQuery.setPageSize(3); // 3 entries per page

            try (QueryCursor<Cache.Entry<Integer, Product>> cursor =
                    cache.query(pagedQuery)) {
                System.out.println("   Processing with page size 3...");
                int pageCount = 0;
                for (Cache.Entry<Integer, Product> entry : cursor) {
                    pageCount++;
                }
                System.out.println("   Processed " + pageCount + " entries");
            }

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Product class for demonstration
    static class Product implements Serializable {
        private String name;
        private String category;
        private double price;

        public Product(String name, String category, double price) {
            this.name = name;
            this.category = category;
            this.price = price;
        }

        public String getName() { return name; }
        public String getCategory() { return category; }
        public double getPrice() { return price; }

        @Override
        public String toString() {
            return name + " (" + category + ") - $" + price;
        }
    }
}
