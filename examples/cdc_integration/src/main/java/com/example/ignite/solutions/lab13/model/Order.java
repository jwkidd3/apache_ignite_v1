package com.example.ignite.solutions.lab13.model;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.time.Instant;

/**
 * Order entity synchronized from PostgreSQL via CDC
 * Uses customer_id as affinity key for colocation with Customer data
 */
public class Order implements Serializable {
    private static final long serialVersionUID = 1L;

    @QuerySqlField(index = true)
    private Integer id;

    @AffinityKeyMapped
    @QuerySqlField(index = true)
    private Integer customerId;

    @QuerySqlField
    private Instant orderDate;

    @QuerySqlField(index = true)
    private String status;

    @QuerySqlField
    private Double totalAmount;

    @QuerySqlField
    private String shippingAddress;

    private Instant createdAt;
    private Instant updatedAt;

    public Order() {}

    public Order(Integer id, Integer customerId, String status, Double totalAmount, String shippingAddress) {
        this.id = id;
        this.customerId = customerId;
        this.orderDate = Instant.now();
        this.status = status;
        this.totalAmount = totalAmount;
        this.shippingAddress = shippingAddress;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }

    public Instant getOrderDate() { return orderDate; }
    public void setOrderDate(Instant orderDate) { this.orderDate = orderDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Double totalAmount) { this.totalAmount = totalAmount; }

    public String getShippingAddress() { return shippingAddress; }
    public void setShippingAddress(String shippingAddress) { this.shippingAddress = shippingAddress; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", customerId=" + customerId +
                ", status='" + status + '\'' +
                ", totalAmount=$" + String.format("%.2f", totalAmount) +
                '}';
    }
}
