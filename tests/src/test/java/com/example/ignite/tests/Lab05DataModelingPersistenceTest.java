package com.example.ignite.tests;

import org.apache.ignite.IgniteCache;
import org.apache.ignite.cache.CacheMode;
import org.apache.ignite.cache.affinity.AffinityKeyMapped;
import org.apache.ignite.cache.query.annotations.QuerySqlField;
import org.apache.ignite.cache.store.CacheStore;
import org.apache.ignite.cache.store.CacheStoreAdapter;
import org.apache.ignite.configuration.CacheConfiguration;
import org.apache.ignite.configuration.DataRegionConfiguration;
import org.apache.ignite.configuration.DataStorageConfiguration;
import org.apache.ignite.configuration.WALMode;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.cache.Cache;
import javax.cache.configuration.Factory;
import javax.cache.integration.CacheLoaderException;
import javax.cache.integration.CacheWriterException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

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

    @Test
    @DisplayName("Test WAL configuration")
    public void testWalConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure WAL mode
        storageCfg.setWalMode(WALMode.LOG_ONLY);
        storageCfg.setWalSegmentSize(64 * 1024 * 1024); // 64 MB segment size
        storageCfg.setWalSegments(4);
        storageCfg.setWalHistorySize(10);

        // Verify WAL configuration
        assertThat(storageCfg.getWalMode()).isEqualTo(WALMode.LOG_ONLY);
        assertThat(storageCfg.getWalSegmentSize()).isEqualTo(64 * 1024 * 1024);
        assertThat(storageCfg.getWalSegments()).isEqualTo(4);
        assertThat(storageCfg.getWalHistorySize()).isEqualTo(10);

        // Test other WAL modes
        storageCfg.setWalMode(WALMode.FSYNC);
        assertThat(storageCfg.getWalMode()).isEqualTo(WALMode.FSYNC);

        storageCfg.setWalMode(WALMode.BACKGROUND);
        assertThat(storageCfg.getWalMode()).isEqualTo(WALMode.BACKGROUND);

        storageCfg.setWalMode(WALMode.NONE);
        assertThat(storageCfg.getWalMode()).isEqualTo(WALMode.NONE);
    }

    @Test
    @DisplayName("Test storage path configuration")
    public void testStoragePathConfiguration() {
        DataStorageConfiguration storageCfg = new DataStorageConfiguration();

        // Configure storage paths
        String storagePath = "/data/ignite/storage";
        String walPath = "/data/ignite/wal";
        String walArchivePath = "/data/ignite/wal-archive";

        storageCfg.setStoragePath(storagePath);
        storageCfg.setWalPath(walPath);
        storageCfg.setWalArchivePath(walArchivePath);

        // Verify path configuration
        assertThat(storageCfg.getStoragePath()).isEqualTo(storagePath);
        assertThat(storageCfg.getWalPath()).isEqualTo(walPath);
        assertThat(storageCfg.getWalArchivePath()).isEqualTo(walArchivePath);

        // Test checkpoint configuration
        storageCfg.setCheckpointFrequency(180000L); // 3 minutes
        assertThat(storageCfg.getCheckpointFrequency()).isEqualTo(180000L);

        // Test page size configuration
        storageCfg.setPageSize(8 * 1024); // 8 KB
        assertThat(storageCfg.getPageSize()).isEqualTo(8 * 1024);
    }

    @Test
    @DisplayName("Test CacheStore configuration with read-through/write-through settings")
    public void testCacheStoreConfiguration() {
        CacheConfiguration<Long, String> cfg = new CacheConfiguration<>(getTestCacheName());

        // Configure cache store factory using a serializable factory
        cfg.setCacheStoreFactory(new TestCacheStoreFactory());

        // Enable read-through and write-through
        cfg.setReadThrough(true);
        cfg.setWriteThrough(true);

        // Verify configuration
        assertThat(cfg.isReadThrough()).isTrue();
        assertThat(cfg.isWriteThrough()).isTrue();
        assertThat(cfg.getCacheStoreFactory()).isNotNull();

        // Create cache to test the configuration is valid
        IgniteCache<Long, String> cache = ignite.getOrCreateCache(cfg);

        // Write-through test: put should write to store
        cache.put(1L, "Value1");
        cache.put(2L, "Value2");

        // Verify values are in cache
        assertThat(cache.get(1L)).isEqualTo("Value1");
        assertThat(cache.get(2L)).isEqualTo("Value2");

        // The cache with store should support load operations
        assertThat(cache.containsKey(1L)).isTrue();
        assertThat(cache.containsKey(2L)).isTrue();
    }

    @Test
    @DisplayName("Test write-behind cache store settings")
    public void testWriteBehindConfiguration() {
        CacheConfiguration<Long, String> cfg = new CacheConfiguration<>(getTestCacheName());

        // Configure write-behind
        cfg.setWriteBehindEnabled(true);
        cfg.setWriteBehindFlushFrequency(5000L); // 5 seconds
        cfg.setWriteBehindFlushSize(1024); // Flush when 1024 entries accumulated
        cfg.setWriteBehindFlushThreadCount(2);
        cfg.setWriteBehindBatchSize(512);

        // Verify write-behind configuration
        assertThat(cfg.isWriteBehindEnabled()).isTrue();
        assertThat(cfg.getWriteBehindFlushFrequency()).isEqualTo(5000L);
        assertThat(cfg.getWriteBehindFlushSize()).isEqualTo(1024);
        assertThat(cfg.getWriteBehindFlushThreadCount()).isEqualTo(2);
        assertThat(cfg.getWriteBehindBatchSize()).isEqualTo(512);

        // Test disabling write-behind
        cfg.setWriteBehindEnabled(false);
        assertThat(cfg.isWriteBehindEnabled()).isFalse();

        // Test default values after re-enabling
        CacheConfiguration<Long, String> cfg2 = new CacheConfiguration<>(getTestCacheName() + "-2");
        cfg2.setWriteBehindEnabled(true);

        // Verify that write-behind can be enabled
        assertThat(cfg2.isWriteBehindEnabled()).isTrue();
    }

    // Serializable CacheStore factory for testing
    static class TestCacheStoreFactory implements Factory<CacheStore<Long, String>>, Serializable {
        private static final long serialVersionUID = 1L;

        @Override
        public CacheStore<Long, String> create() {
            return new TestCacheStore();
        }
    }

    // Test CacheStore implementation for testing
    static class TestCacheStore extends CacheStoreAdapter<Long, String> implements Serializable {
        private static final long serialVersionUID = 1L;
        private final Map<Long, String> backingStore = new ConcurrentHashMap<>();

        public Map<Long, String> getBackingStore() {
            return backingStore;
        }

        @Override
        public String load(Long key) throws CacheLoaderException {
            return backingStore.get(key);
        }

        @Override
        public void write(Cache.Entry<? extends Long, ? extends String> entry) throws CacheWriterException {
            backingStore.put(entry.getKey(), entry.getValue());
        }

        @Override
        public void delete(Object key) throws CacheWriterException {
            backingStore.remove(key);
        }
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
