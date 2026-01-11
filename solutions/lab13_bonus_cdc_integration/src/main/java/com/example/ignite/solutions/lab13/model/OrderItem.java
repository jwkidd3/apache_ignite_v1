package com.example.ignite.solutions.lab13.model;

import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.time.Instant;

/**
 * OrderItem entity synchronized from PostgreSQL via CDC
 * Uses order_id as affinity key for colocation with Order data
 */
public class OrderItem implements Serializable {
    private static final long serialVersionUID = 1L;

    @QuerySqlField(index = true)
    private Integer id;

    @AffinityKeyMapped
    @QuerySqlField(index = true)
    private Integer orderId;

    @QuerySqlField(index = true)
    private Integer productId;

    @QuerySqlField
    private Integer quantity;

    @QuerySqlField
    private Double unitPrice;

    private Instant createdAt;

    public OrderItem() {}

    public OrderItem(Integer id, Integer orderId, Integer productId, Integer quantity, Double unitPrice) {
        this.id = id;
        this.orderId = orderId;
        this.productId = productId;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.createdAt = Instant.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public Integer getOrderId() { return orderId; }
    public void setOrderId(Integer orderId) { this.orderId = orderId; }

    public Integer getProductId() { return productId; }
    public void setProductId(Integer productId) { this.productId = productId; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public Double getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Double unitPrice) { this.unitPrice = unitPrice; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Double getLineTotal() {
        return quantity * unitPrice;
    }

    @Override
    public String toString() {
        return "OrderItem{" +
                "id=" + id +
                ", orderId=" + orderId +
                ", productId=" + productId +
                ", quantity=" + quantity +
                ", unitPrice=$" + String.format("%.2f", unitPrice) +
                ", lineTotal=$" + String.format("%.2f", getLineTotal()) +
                '}';
    }
}
