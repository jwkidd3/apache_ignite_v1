package com.example.ignite.solutions.lab05;

import com.example.ignite.solutions.lab05.model.Customer;
import com.example.ignite.solutions.lab05.model.Order;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.affinity.Affinity;
import org.apache.ignite.cluster.ClusterNode;
import org.apache.ignite.configuration.CacheConfiguration;

/**
 * Lab 5 Exercise 2: Affinity Keys and Colocation
 *
 * This exercise demonstrates using affinity keys to ensure
 * related data is stored on the same node.
 */
public class AffinityKeys {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Affinity Keys and Colocation Lab ===\n");

            // Create caches
            CacheConfiguration<Integer, Customer> customerCfg =
                new CacheConfiguration<>("customers");
            customerCfg.setBackups(1);

            CacheConfiguration<Integer, Order> orderCfg =
                new CacheConfiguration<>("orders");
            orderCfg.setBackups(1);

            IgniteCache<Integer, Customer> customerCache =
                ignite.getOrCreateCache(customerCfg);
            IgniteCache<Integer, Order> orderCache =
                ignite.getOrCreateCache(orderCfg);

            // Create test data
            Customer customer1 = new Customer(1, "John Doe",
                "john@example.com", "New York");
            Customer customer2 = new Customer(2, "Jane Smith",
                "jane@example.com", "Los Angeles");

            Order order1 = new Order(101, 1, "Laptop", 1200.00);
            Order order2 = new Order(102, 1, "Mouse", 25.00);
            Order order3 = new Order(103, 2, "Keyboard", 75.00);

            // Store data
            customerCache.put(1, customer1);
            customerCache.put(2, customer2);
            orderCache.put(101, order1);
            orderCache.put(102, order2);
            orderCache.put(103, order3);

            System.out.println("Stored customers and orders");

            // Check affinity - verify colocation
            Affinity<Integer> customerAffinity = ignite.affinity("customers");
            Affinity<Integer> orderAffinity = ignite.affinity("orders");

            System.out.println("\n=== Affinity Verification ===");

            // Customer 1 and their orders
            ClusterNode customer1Node = customerAffinity.mapKeyToNode(1);
            ClusterNode order1Node = orderAffinity.mapKeyToNode(101);
            ClusterNode order2Node = orderAffinity.mapKeyToNode(102);

            System.out.println("Customer 1 on node: " +
                customer1Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 101 on node: " +
                order1Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 102 on node: " +
                order2Node.id().toString().substring(0, 8) + "...");

            System.out.println("\nColocated (Customer 1 with Orders): " +
                (customer1Node.equals(order1Node) && customer1Node.equals(order2Node)));

            // Customer 2 and their orders
            ClusterNode customer2Node = customerAffinity.mapKeyToNode(2);
            ClusterNode order3Node = orderAffinity.mapKeyToNode(103);

            System.out.println("\nCustomer 2 on node: " +
                customer2Node.id().toString().substring(0, 8) + "...");
            System.out.println("Order 103 on node: " +
                order3Node.id().toString().substring(0, 8) + "...");
            System.out.println("Colocated (Customer 2 with Order): " +
                customer2Node.equals(order3Node));

            // Display partition information
            System.out.println("\n=== Partition Information ===");
            System.out.println("Customer 1 partition: " + customerAffinity.partition(1));
            System.out.println("Order 101 partition: " + orderAffinity.partition(101));
            System.out.println("Order 102 partition: " + orderAffinity.partition(102));

            System.out.println("\n=== Benefits of Colocation ===");
            System.out.println("- Reduced network hops for joins");
            System.out.println("- Better performance for related data access");
            System.out.println("- Efficient co-located processing");
            System.out.println("- Transactional consistency for related entities");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
