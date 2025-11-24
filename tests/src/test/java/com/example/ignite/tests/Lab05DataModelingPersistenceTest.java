package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.Serializable;

import static org.assertj.core.api.Assertions.*;

/**
 * Comprehensive tests for Lab 5: Data Modeling and Persistence
 * Coverage: Affinity keys, colocation, persistence configuration
 */
@DisplayName("Lab 05: Data Modeling and Persistence Tests")
public class Lab05DataModelingPersistenceTest extends BaseIgniteTest {

    @Test
    @DisplayName("Test affinity key annotation")
    public void testAffinityKeyAnnotation() {
        CacheConfiguration<PersonKey, Person> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<PersonKey, Person> cache = ignite.getOrCreateCache(cfg);

        PersonKey key = new PersonKey(1, 100);
        Person person = new Person("John Doe", 30);

        cache.put(key, person);

        assertThat(cache.get(key)).isEqualTo(person);
    }

    @Test
    @DisplayName("Test data colocation with affinity keys")
    public void testDataColocation() {
        CacheConfiguration<PersonKey, Person> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<PersonKey, Person> cache = ignite.getOrCreateCache(cfg);

        // Same companyId should be colocated
        PersonKey key1 = new PersonKey(1, 100);
        PersonKey key2 = new PersonKey(2, 100);

        cache.put(key1, new Person("Person1", 25));
        cache.put(key2, new Person("Person2", 30));

        // Check affinity
        int partition1 = ignite.affinity(getTestCacheName()).partition(key1);
        int partition2 = ignite.affinity(getTestCacheName()).partition(key2);

        assertThat(partition1).isEqualTo(partition2);
    }

    @Test
    @DisplayName("Test persistence configuration")
    public void testPersistenceConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setPersistenceEnabled(false); // For testing, use in-memory
        regionCfg.setMaxSize(100L * 1024 * 1024); // 100 MB

        storageCfg.setDefaultDataRegionConfiguration(regionCfg);

        assertThat(regionCfg.getMaxSize()).isEqualTo(100L * 1024 * 1024);
    }

    @Test
    @DisplayName("Test cache with custom key class")
    public void testCustomKeyClass() {
        CacheConfiguration<PersonKey, String> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<PersonKey, String> cache = ignite.getOrCreateCache(cfg);

        PersonKey key = new PersonKey(1, 100);
        cache.put(key, "Test Value");

        assertThat(cache.get(key)).isEqualTo("Test Value");
    }

    @Test
    @DisplayName("Test affinity mapping for multiple companies")
    public void testAffinityMapping() {
        CacheConfiguration<PersonKey, Person> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<PersonKey, Person> cache = ignite.getOrCreateCache(cfg);

        // Company 100
        PersonKey key1 = new PersonKey(1, 100);
        PersonKey key2 = new PersonKey(2, 100);

        // Company 200
        PersonKey key3 = new PersonKey(3, 200);
        PersonKey key4 = new PersonKey(4, 200);

        int partition1 = ignite.affinity(getTestCacheName()).partition(key1);
        int partition2 = ignite.affinity(getTestCacheName()).partition(key2);
        int partition3 = ignite.affinity(getTestCacheName()).partition(key3);
        int partition4 = ignite.affinity(getTestCacheName()).partition(key4);

        // Same company ID should map to same partition
        assertThat(partition1).isEqualTo(partition2);
        assertThat(partition3).isEqualTo(partition4);
    }

    @Test
    @DisplayName("Test cache operations with complex keys")
    public void testComplexKeyOperations() {
        CacheConfiguration<PersonKey, Person> cfg = new CacheConfiguration<>(getTestCacheName());

        IgniteCache<PersonKey, Person> cache = ignite.getOrCreateCache(cfg);

        PersonKey key1 = new PersonKey(1, 100);
        PersonKey key2 = new PersonKey(2, 100);
        PersonKey key3 = new PersonKey(3, 200);

        cache.put(key1, new Person("Alice", 25));
        cache.put(key2, new Person("Bob", 30));
        cache.put(key3, new Person("Charlie", 35));

        assertThat(cache.size()).isEqualTo(3);
        assertThat(cache.get(key1).getName()).isEqualTo("Alice");
        assertThat(cache.get(key2).getName()).isEqualTo("Bob");
        assertThat(cache.get(key3).getName()).isEqualTo("Charlie");
    }

    @Test
    @DisplayName("Test data region configuration")
    public void testDataRegionConfig() {
        DataRegionConfiguration regionCfg = new DataRegionConfiguration();
        regionCfg.setName("testRegion");
        regionCfg.setInitialSize(50L * 1024 * 1024);
        regionCfg.setMaxSize(200L * 1024 * 1024);

        assertThat(regionCfg.getName()).isEqualTo("testRegion");
        assertThat(regionCfg.getInitialSize()).isEqualTo(50L * 1024 * 1024);
        assertThat(regionCfg.getMaxSize()).isEqualTo(200L * 1024 * 1024);
    }

    // Test classes
    static class PersonKey implements Serializable {
        private int personId;

        @AffinityKeyMapped
        private int companyId;

        public PersonKey(int personId, int companyId) {
            this.personId = personId;
            this.companyId = companyId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PersonKey)) return false;
            PersonKey that = (PersonKey) o;
            return personId == that.personId && companyId == that.companyId;
        }

        @Override
        public int hashCode() {
            return 31 * personId + companyId;
        }
    }

    static class Person implements Serializable {
        @QuerySqlField
        private String name;

        @QuerySqlField
        private int age;

        public Person(String name, int age) {
            this.name = name;
            this.age = age;
        }

        public String getName() {
            return name;
        }

        public int getAge() {
            return age;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof Person)) return false;
            Person person = (Person) o;
            return age == person.age && name.equals(person.name);
        }

        @Override
        public int hashCode() {
            return 31 * name.hashCode() + age;
        }
    }
}
