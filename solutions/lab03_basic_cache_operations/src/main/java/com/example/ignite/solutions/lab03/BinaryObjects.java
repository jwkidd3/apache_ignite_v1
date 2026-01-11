package com.example.ignite.solutions.lab03;

import org.apache.ignite.Ignite;
import org.apache.ignite.IgniteCache;
import org.apache.ignite.Ignition;
import org.apache.ignite.binary.BinaryObject;
import org.apache.ignite.binary.BinaryObjectBuilder;
import org.apache.ignite.cache.query.QueryCursor;
import org.apache.ignite.cache.query.ScanQuery;
import org.apache.ignite.configuration.CacheConfiguration;

import javax.cache.Cache;
import java.io.Serializable;

/**
 * Lab 3 Optional: Working with Binary Objects
 *
 * This exercise demonstrates using Binary Objects for
 * schema-less data access and field-level operations.
 */
public class BinaryObjects {

    public static void main(String[] args) {
        try (Ignite ignite = Ignition.start()) {
            System.out.println("=== Binary Objects and Keep Binary Lab ===\n");

            CacheConfiguration<Integer, Person> cfg =
                new CacheConfiguration<>("personCache");
            IgniteCache<Integer, Person> cache = ignite.getOrCreateCache(cfg);

            // 1. Store regular objects
            System.out.println("1. Storing Regular Objects:");
            cache.put(1, new Person("Alice", 30, "Engineering"));
            cache.put(2, new Person("Bob", 25, "Marketing"));
            cache.put(3, new Person("Charlie", 35, "Engineering"));
            System.out.println("   Stored 3 Person objects");

            // 2. Retrieve as regular objects
            System.out.println("\n2. Retrieve as Regular Objects:");
            Person alice = cache.get(1);
            System.out.println("   Person 1: " + alice);
            System.out.println("   Class: " + alice.getClass().getName());

            // 3. Get cache in "keep binary" mode
            System.out.println("\n3. Using withKeepBinary():");
            IgniteCache<Integer, BinaryObject> binaryCache = cache.withKeepBinary();

            BinaryObject binaryAlice = binaryCache.get(1);
            System.out.println("   Binary Object retrieved");
            System.out.println("   Type: " + binaryAlice.type().typeName());
            System.out.println("   Fields: " + binaryAlice.type().fieldNames());

            // 4. Access fields from BinaryObject
            System.out.println("\n4. Accessing Fields from BinaryObject:");
            String name = binaryAlice.field("name");
            int age = binaryAlice.field("age");
            String dept = binaryAlice.field("department");
            System.out.println("   Name: " + name);
            System.out.println("   Age: " + age);
            System.out.println("   Department: " + dept);

            // 5. Benefits: No deserialization cost
            System.out.println("\n5. Performance Benefit - Field Access Without Deserialization:");
            long startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                Person p = cache.get(1);
                String n = p.getName();
            }
            long regularTime = System.currentTimeMillis() - startTime;
            System.out.println("   Regular access (1000 iterations): " + regularTime + " ms");

            startTime = System.currentTimeMillis();
            for (int i = 0; i < 1000; i++) {
                BinaryObject bo = binaryCache.get(1);
                String n = bo.field("name");
            }
            long binaryTime = System.currentTimeMillis() - startTime;
            System.out.println("   Binary access (1000 iterations): " + binaryTime + " ms");

            // 6. Create BinaryObject without class definition
            System.out.println("\n6. Creating BinaryObject with Builder (no class needed):");
            BinaryObjectBuilder builder = ignite.binary().builder("Employee");
            builder.setField("id", 100);
            builder.setField("name", "Dynamic Employee");
            builder.setField("salary", 75000.0);
            builder.setField("active", true);

            BinaryObject employee = builder.build();
            binaryCache.put(100, employee);
            System.out.println("   Created and stored Employee BinaryObject");

            BinaryObject retrieved = binaryCache.get(100);
            System.out.println("   Retrieved - Name: " + retrieved.field("name"));
            System.out.println("   Retrieved - Salary: " + retrieved.field("salary"));

            // 7. Modify BinaryObject
            System.out.println("\n7. Modifying BinaryObject:");
            BinaryObject bobBinary = binaryCache.get(2);

            // Create new object from existing with modifications
            BinaryObject modifiedBob = bobBinary.toBuilder()
                .setField("age", 26)  // Birthday!
                .setField("department", "Sales")  // Department change
                .build();

            binaryCache.put(2, modifiedBob);
            System.out.println("   Modified Bob's age and department");

            BinaryObject updatedBob = binaryCache.get(2);
            System.out.println("   New age: " + updatedBob.field("age"));
            System.out.println("   New department: " + updatedBob.field("department"));

            // 8. Query with BinaryObjects
            System.out.println("\n8. ScanQuery with BinaryObjects:");
            try (QueryCursor<Cache.Entry<Integer, BinaryObject>> cursor =
                    binaryCache.query(new ScanQuery<>(
                        (k, v) -> "Engineering".equals(v.field("department"))))) {

                System.out.println("   Engineering employees:");
                for (Cache.Entry<Integer, BinaryObject> entry : cursor) {
                    BinaryObject bo = entry.getValue();
                    System.out.println("   - " + bo.field("name") +
                        " (age: " + bo.field("age") + ")");
                }
            }

            System.out.println("\n=== Binary Objects Benefits ===");
            System.out.println("- No need for class on server side");
            System.out.println("- Field access without full deserialization");
            System.out.println("- Dynamic schema - add fields at runtime");
            System.out.println("- Efficient for partial reads");
            System.out.println("- Cross-platform compatibility");

            System.out.println("\nPress Enter to exit...");
            System.in.read();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // Person class for demonstration
    static class Person implements Serializable {
        private String name;
        private int age;
        private String department;

        public Person(String name, int age, String department) {
            this.name = name;
            this.age = age;
            this.department = department;
        }

        public String getName() { return name; }
        public int getAge() { return age; }
        public String getDepartment() { return department; }

        @Override
        public String toString() {
            return name + " (age: " + age + ", dept: " + department + ")";
        }
    }
}
