package com.example.ignite.solutions.lab13.cdc;

import com.example.ignite.solutions.lab13.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.kafka.clients.consumer.*;
import org.apache.kafka.common.serialization.StringDeserializer;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Kafka Consumer that processes Debezium CDC events and updates Apache Ignite caches.
 *
 * Architecture:
 * PostgreSQL -> Debezium -> Kafka -> This Consumer -> Apache Ignite
 *
 * Supports:
 * - INSERT (op=c) and snapshot (op=r): Add to Ignite cache
 * - UPDATE (op=u): Update in Ignite cache
 * - DELETE (op=d): Remove from Ignite cache
 */
public class IgniteCDCConsumer implements Runnable {

    private final Ignite ignite;
    private final String bootstrapServers;
    private final List<String> topics;
    private final AtomicBoolean running = new AtomicBoolean(true);
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Metrics
    private final AtomicLong processedEvents = new AtomicLong(0);
    private final AtomicLong insertCount = new AtomicLong(0);
    private final AtomicLong updateCount = new AtomicLong(0);
    private final AtomicLong deleteCount = new AtomicLong(0);
    private final AtomicLong errorCount = new AtomicLong(0);

    // Caches
    private IgniteCache<Integer, Customer> customerCache;
    private IgniteCache<Integer, Product> productCache;
    private IgniteCache<Integer, Order> orderCache;
    private IgniteCache<Integer, OrderItem> orderItemCache;

    public IgniteCDCConsumer(Ignite ignite, String bootstrapServers, List<String> topics) {
        this.ignite = ignite;
        this.bootstrapServers = bootstrapServers;
        this.topics = topics;

        // Get or create caches
        this.customerCache = ignite.getOrCreateCache("customers");
        this.productCache = ignite.getOrCreateCache("products");
        this.orderCache = ignite.getOrCreateCache("orders");
        this.orderItemCache = ignite.getOrCreateCache("order_items");
    }

    @Override
    public void run() {
        Properties props = new Properties();
        props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
        props.put(ConsumerConfig.GROUP_ID_CONFIG, "ignite-cdc-consumer");
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, "true");
        props.put(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG, "1000");

        try (KafkaConsumer<String, String> consumer = new KafkaConsumer<>(props)) {
            consumer.subscribe(topics);

            System.out.println("=== CDC Consumer Started ===");
            System.out.println("Subscribed to topics: " + topics);
            System.out.println("Waiting for CDC events...\n");

            while (running.get()) {
                ConsumerRecords<String, String> records = consumer.poll(Duration.ofMillis(1000));

                for (ConsumerRecord<String, String> record : records) {
                    try {
                        processRecord(record);
                        processedEvents.incrementAndGet();
                    } catch (Exception e) {
                        errorCount.incrementAndGet();
                        System.err.println("Error processing record: " + e.getMessage());
                        e.printStackTrace();
                    }
                }
            }
        }

        System.out.println("\n=== CDC Consumer Stopped ===");
        printMetrics();
    }

    private void processRecord(ConsumerRecord<String, String> record) throws Exception {
        String topic = record.topic();
        String value = record.value();

        if (value == null || value.isEmpty()) {
            return; // Tombstone message
        }

        CDCEvent event = objectMapper.readValue(value, CDCEvent.class);
        String table = extractTableFromTopic(topic);

        System.out.printf("[CDC] %s | Table: %s | Op: %s%n",
                Instant.ofEpochMilli(event.getTimestamp() != null ? event.getTimestamp() : System.currentTimeMillis()),
                table,
                event.getOp());

        switch (table) {
            case "customers":
                processCustomerEvent(event);
                break;
            case "products":
                processProductEvent(event);
                break;
            case "orders":
                processOrderEvent(event);
                break;
            case "order_items":
                processOrderItemEvent(event);
                break;
            default:
                System.out.println("  Unknown table: " + table);
        }
    }

    private String extractTableFromTopic(String topic) {
        // Topic format: dbserver1.inventory.customers
        String[] parts = topic.split("\\.");
        return parts.length > 2 ? parts[2] : topic;
    }

    private void processCustomerEvent(CDCEvent event) {
        if (event.isInsert() || event.isUpdate()) {
            Map<String, Object> data = event.getAfter();
            if (data == null) return;

            Customer customer = new Customer();
            customer.setId(getInt(data, "id"));
            customer.setFirstName(getString(data, "first_name"));
            customer.setLastName(getString(data, "last_name"));
            customer.setEmail(getString(data, "email"));
            customer.setCity(getString(data, "city"));

            customerCache.put(customer.getId(), customer);
            System.out.println("  -> Ignite: " + (event.isInsert() ? "INSERT" : "UPDATE") + " " + customer);

            if (event.isInsert()) insertCount.incrementAndGet();
            else updateCount.incrementAndGet();

        } else if (event.isDelete()) {
            Map<String, Object> data = event.getBefore();
            if (data == null) return;

            Integer id = getInt(data, "id");
            customerCache.remove(id);
            System.out.println("  -> Ignite: DELETE customer id=" + id);
            deleteCount.incrementAndGet();
        }
    }

    private void processProductEvent(CDCEvent event) {
        if (event.isInsert() || event.isUpdate()) {
            Map<String, Object> data = event.getAfter();
            if (data == null) return;

            Product product = new Product();
            product.setId(getInt(data, "id"));
            product.setName(getString(data, "name"));
            product.setDescription(getString(data, "description"));
            product.setPrice(getDouble(data, "price"));
            product.setQuantity(getInt(data, "quantity"));
            product.setCategory(getString(data, "category"));

            productCache.put(product.getId(), product);
            System.out.println("  -> Ignite: " + (event.isInsert() ? "INSERT" : "UPDATE") + " " + product);

            if (event.isInsert()) insertCount.incrementAndGet();
            else updateCount.incrementAndGet();

        } else if (event.isDelete()) {
            Map<String, Object> data = event.getBefore();
            if (data == null) return;

            Integer id = getInt(data, "id");
            productCache.remove(id);
            System.out.println("  -> Ignite: DELETE product id=" + id);
            deleteCount.incrementAndGet();
        }
    }

    private void processOrderEvent(CDCEvent event) {
        if (event.isInsert() || event.isUpdate()) {
            Map<String, Object> data = event.getAfter();
            if (data == null) return;

            Order order = new Order();
            order.setId(getInt(data, "id"));
            order.setCustomerId(getInt(data, "customer_id"));
            order.setStatus(getString(data, "status"));
            order.setTotalAmount(getDouble(data, "total_amount"));
            order.setShippingAddress(getString(data, "shipping_address"));

            orderCache.put(order.getId(), order);
            System.out.println("  -> Ignite: " + (event.isInsert() ? "INSERT" : "UPDATE") + " " + order);

            if (event.isInsert()) insertCount.incrementAndGet();
            else updateCount.incrementAndGet();

        } else if (event.isDelete()) {
            Map<String, Object> data = event.getBefore();
            if (data == null) return;

            Integer id = getInt(data, "id");
            orderCache.remove(id);
            System.out.println("  -> Ignite: DELETE order id=" + id);
            deleteCount.incrementAndGet();
        }
    }

    private void processOrderItemEvent(CDCEvent event) {
        if (event.isInsert() || event.isUpdate()) {
            Map<String, Object> data = event.getAfter();
            if (data == null) return;

            OrderItem item = new OrderItem();
            item.setId(getInt(data, "id"));
            item.setOrderId(getInt(data, "order_id"));
            item.setProductId(getInt(data, "product_id"));
            item.setQuantity(getInt(data, "quantity"));
            item.setUnitPrice(getDouble(data, "unit_price"));

            orderItemCache.put(item.getId(), item);
            System.out.println("  -> Ignite: " + (event.isInsert() ? "INSERT" : "UPDATE") + " " + item);

            if (event.isInsert()) insertCount.incrementAndGet();
            else updateCount.incrementAndGet();

        } else if (event.isDelete()) {
            Map<String, Object> data = event.getBefore();
            if (data == null) return;

            Integer id = getInt(data, "id");
            orderItemCache.remove(id);
            System.out.println("  -> Ignite: DELETE order_item id=" + id);
            deleteCount.incrementAndGet();
        }
    }

    // Helper methods for type conversion
    private Integer getInt(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Integer) return (Integer) value;
        return Integer.parseInt(value.toString());
    }

    private String getString(Map<String, Object> data, String key) {
        Object value = data.get(key);
        return value != null ? value.toString() : null;
    }

    private Double getDouble(Map<String, Object> data, String key) {
        Object value = data.get(key);
        if (value == null) return null;
        if (value instanceof Double) return (Double) value;
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(value.toString());
    }

    public void stop() {
        running.set(false);
    }

    public void printMetrics() {
        System.out.println("\n=== CDC Consumer Metrics ===");
        System.out.println("Total processed: " + processedEvents.get());
        System.out.println("Inserts: " + insertCount.get());
        System.out.println("Updates: " + updateCount.get());
        System.out.println("Deletes: " + deleteCount.get());
        System.out.println("Errors: " + errorCount.get());
    }

    public void printCacheStats() {
        System.out.println("\n=== Ignite Cache Statistics ===");
        System.out.println("Customers: " + customerCache.size() + " entries");
        System.out.println("Products: " + productCache.size() + " entries");
        System.out.println("Orders: " + orderCache.size() + " entries");
        System.out.println("Order Items: " + orderItemCache.size() + " entries");
    }
}
