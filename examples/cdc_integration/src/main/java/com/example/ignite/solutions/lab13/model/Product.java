package com.example.ignite.solutions.lab13.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;
import java.time.Instant;

/**
 * Product entity synchronized from PostgreSQL via CDC
 */
public class Product implements Serializable {
    private static final long serialVersionUID = 1L;

    @QuerySqlField(index = true)
    private Integer id;

    @QuerySqlField
    private String name;

    @QuerySqlField
    private String description;

    @QuerySqlField
    private Double price;

    @QuerySqlField
    private Integer quantity;

    @QuerySqlField(index = true)
    private String category;

    private Instant createdAt;
    private Instant updatedAt;

    public Product() {}

    public Product(Integer id, String name, String description, Double price, Integer quantity, String category) {
        this.id = id;
        this.name = name;
        this.description = description;
        this.price = price;
        this.quantity = quantity;
        this.category = category;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
    }

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }

    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }

    public boolean isInStock() {
        return quantity != null && quantity > 0;
    }

    @Override
    public String toString() {
        return "Product{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", price=$" + String.format("%.2f", price) +
                ", quantity=" + quantity +
                ", category='" + category + '\'' +
                '}';
    }
}
