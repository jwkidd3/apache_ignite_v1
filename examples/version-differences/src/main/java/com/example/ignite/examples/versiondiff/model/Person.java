package com.example.ignite.examples.versiondiff.model;

import org.apache.ignite.cache.query.annotations.QuerySqlField;

import java.io.Serializable;

/**
 * Person entity demonstrating Ignite 2.16 schema definition via annotations.
 *
 * In Ignite 2.16, schema is defined using @QuerySqlField annotations.
 * In Ignite 3.x, schema is defined via SQL DDL (CREATE TABLE).
 */
public class Person implements Serializable {

    private static final long serialVersionUID = 1L;

    @QuerySqlField(index = true)
    private int id;

    @QuerySqlField
    private String firstName;

    @QuerySqlField(index = true)
    private String lastName;

    @QuerySqlField
    private int age;

    public Person() {
    }

    public Person(int id, String firstName, String lastName, int age) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.age = age;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    public int getAge() { return age; }
    public void setAge(int age) { this.age = age; }

    @Override
    public String toString() {
        return String.format("Person[id=%d, name=%s %s, age=%d]", id, firstName, lastName, age);
    }
}
