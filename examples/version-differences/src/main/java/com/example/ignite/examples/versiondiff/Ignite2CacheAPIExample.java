package com.example.ignite.examples.versiondiff;

import com.example.ignite.examples.versiondiff.model.Person;
import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.SqlFieldsQuery;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.IgniteConfiguration;

import java.util.List;

/**
 * Demonstrates Ignite 2.16 Cache API (cache-first approach).
 *
 * Compare with Ignite 3.x Table API (schema-first approach):
 *
 * // Ignite 3.x
 * client.sql().execute(null, "CREATE TABLE Person (...)");
 * Table table = client.tables().table("Person");
 * KeyValueView<Integer, Person> kv = table.keyValueView(...);
 * kv.put(null, 1, person);
 */
public class Ignite2CacheAPIExample {

    public static void main(String[] args) {
        System.out.println("=== Ignite 2.16 Cache API Example ===\n");

        IgniteConfiguration cfg = new IgniteConfiguration();
        cfg.setIgniteInstanceName("ignite2-cache-demo");
        cfg.setPeerClassLoadingEnabled(true);

        CacheConfiguration<Integer, Person> personCacheCfg = new CacheConfiguration<>("PersonCache");
        personCacheCfg.setIndexedTypes(Integer.class, Person.class);
        cfg.setCacheConfiguration(personCacheCfg);

        try (Ignite ignite = Ignition.start(cfg)) {
            IgniteCache<Integer, Person> cache = ignite.cache("PersonCache");

            // CREATE
            System.out.println("1. CREATE (cache.put)");
            cache.put(1, new Person(1, "John", "Doe", 30));
            cache.put(2, new Person(2, "Jane", "Smith", 28));
            System.out.println("   Created 2 persons\n");

            // READ
            System.out.println("2. READ (cache.get)");
            Person person = cache.get(1);
            System.out.println("   " + person + "\n");

            // UPDATE
            System.out.println("3. UPDATE (cache.put)");
            person.setAge(31);
            cache.put(1, person);
            System.out.println("   Updated: " + cache.get(1) + "\n");

            // SQL Query
            System.out.println("4. SQL Query (cache.query)");
            SqlFieldsQuery query = new SqlFieldsQuery(
                "SELECT id, firstName, lastName, age FROM Person WHERE age > ?");
            query.setArgs(25);

            try (QueryCursor<List<?>> cursor = cache.query(query)) {
                for (List<?> row : cursor) {
                    System.out.printf("   ID: %d, Name: %s %s, Age: %d%n",
                        row.get(0), row.get(1), row.get(2), row.get(3));
                }
            }

            System.out.println("\nPress Enter to stop...");
            System.in.read();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
