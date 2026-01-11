package com.example.ignite.solutions.lab05.model;

import java.io.Serializable;

/**
 * Lab 5 Exercise 1: Customer Domain Model
 *
 * Basic domain class for customer data.
 */
public class Customer implements Serializable {
    private Integer customerId;
    private String name;
    private String email;
    private String city;

    public Customer() {}

    public Customer(Integer customerId, String name, String email, String city) {
        this.customerId = customerId;
        this.name = name;
        this.email = email;
        this.city = city;
    }

    // Getters and setters
    public Integer getCustomerId() { return customerId; }
    public void setCustomerId(Integer customerId) { this.customerId = customerId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }

    @Override
    public String toString() {
        return "Customer{" +
                "customerId=" + customerId +
                ", name='" + name + '\'' +
                ", email='" + email + '\'' +
                ", city='" + city + '\'' +
                '}';
    }
}
