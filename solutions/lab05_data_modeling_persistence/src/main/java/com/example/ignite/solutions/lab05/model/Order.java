package com.example.ignite.solutions.lab05.model;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import java.io.Serializable;

/**
 * Lab 5 Exercise 1: Order Domain Model with Affinity Key
 *
 * Domain class with @AffinityKeyMapped annotation to ensure
 * orders are colocated with their customers.
 */
public class Order implements Serializable {
    private Integer orderId;

    @AffinityKeyMapped
    private Integer customerId;  // Affinity key - ensures colocation with Customer

    private String product;
    private Double amount;

    public Order() {}

    public Order(Integer orderId, Integer customerId, String product, Double amount) {
        this.orderId = orderId;
        this.customerId = customerId;
        this.product = product;
        this.amount = amount;
    }

    // Getters and setters
    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getProduct() { return product; }
    public void setProduct(String product) { this.product = product; }
    public Double getAmount() { return amount; }
    public void setAmount(Double amount) { this.amount = amount; }

    @Override
    public String toString() {
        return "Order{" +
                "orderId=" + orderId +
                ", customerId=" + customerId +
                ", product='" + product + '\'' +
                ", amount=" + amount +
                '}';
    }
}
