# Student Lab Validation Guide

## Purpose

This test suite validates that your lab solutions implement all required operations correctly. Each test corresponds to specific exercises you'll complete in the labs.

## How to Use This Test Suite

### Step 1: Complete a Lab Exercise

Work through the lab exercises in your own code (not in this test directory).

### Step 2: Run Corresponding Tests to Validate

After completing each lab, run the tests to verify your implementation:

```bash
cd /Users/jwkidd3/classes_in_development/apache_ignite_v1/tests

# After completing Lab 1
mvn test -Dtest=Lab01EnvironmentSetupTest

# After completing Lab 2
mvn test -Dtest=Lab02MultiNodeClusterTest

# And so on...
```

### Step 3: Review Test Results

✅ **All tests pass**: Your lab implementation is correct!
❌ **Some tests fail**: Review the failed tests to see what operations are missing or incorrect.

## Lab-by-Lab Validation

### Lab 1: Environment Setup

**What you'll do in the lab:**
1. Start an Ignite node
2. Create a cache
3. Perform basic put/get operations
4. Test remove, clear, replace operations

**Run validation:**
```bash
mvn test -Dtest=Lab01EnvironmentSetupTest
```

**Tests validate:**
- ✅ Node starts successfully (testNodeStartup)
- ✅ Cache creation works (testCacheCreation)
- ✅ Put operation stores data (testPutOperation)
- ✅ Get operation retrieves data (testGetOperation)
- ✅ Remove operation deletes data (testRemoveOperation)
- ✅ Replace operation updates data (testReplaceOperation)
- ✅ PutIfAbsent works correctly (testPutIfAbsent)
- ✅ ContainsKey checks existence (testContainsKey)

**Expected result:** 16/16 tests pass ✅

---

### Lab 2: Multi-Node Cluster

**What you'll do in the lab:**
1. Start multiple Ignite nodes
2. Verify cluster formation
3. Test data replication with backups
4. Check data is accessible from all nodes

**Run validation:**
```bash
mvn test -Dtest=Lab02MultiNodeClusterTest
```

**Tests validate:**
- ✅ Second node joins cluster (testSecondNodeJoin)
- ✅ Three-node cluster forms (testThreeNodeCluster)
- ✅ All nodes see topology (testClusterTopology)
- ✅ Cache accessible from multiple nodes (testCacheAccessFromMultipleNodes)
- ✅ Data replicates with backups (testDataReplication)
- ✅ Node leave detected (testNodeLeave)
- ✅ Data remains consistent (testDataConsistency)

**Expected result:** 14/14 tests pass ✅

---

### Lab 3: Basic Cache Operations

**What you'll do in the lab:**
1. Create caches with different modes (PARTITIONED, REPLICATED, LOCAL)
2. Test atomic and transactional operations
3. Use async operations
4. Perform batch operations (putAll, getAll)

**Run validation:**
```bash
mvn test -Dtest=Lab03BasicCacheOperationsTest
```

**Tests validate:**
- ✅ All cache modes work (testPartitionedCache, testReplicatedCache, testLocalCache)
- ✅ Atomic operations (testAtomicOperations)
- ✅ Async put/get (testAsyncPut, testAsyncGet)
- ✅ Batch operations (testPutAll, testGetAll, testRemoveAll)
- ✅ Batch faster than individual (testBatchPerformance)
- ✅ Replace with value check (testReplaceIfEquals)

**Expected result:** 23/23 tests pass ✅

---

### Lab 4: Configuration and Deployment

**What you'll do in the lab:**
1. Configure Ignite programmatically
2. Enable cache statistics
3. Monitor cache and cluster metrics
4. Configure data regions

**Run validation:**
```bash
mvn test -Dtest=Lab04ConfigurationDeploymentTest
```

**Tests validate:**
- ✅ Programmatic config works (testProgrammaticConfiguration)
- ✅ Cache statistics enabled (testCacheMetrics)
- ✅ Hit/miss tracking (testCacheHitMissStats)
- ✅ Cluster metrics available (testClusterMetrics)
- ✅ Timing metrics collected (testCacheTimingMetrics)
- ✅ Data region metrics (testDataRegionMetrics)

**Expected result:** 20/20 tests pass ✅

---

### Lab 5: Data Modeling and Persistence

**What you'll do in the lab:**
1. Create classes with affinity keys
2. Test data colocation
3. Use custom key classes
4. Configure persistence

**Run validation:**
```bash
mvn test -Dtest=Lab05DataModelingPersistenceTest
```

**Tests validate:**
- ✅ Affinity key annotation works (testAffinityKeyAnnotation)
- ✅ Data colocates correctly (testDataColocation)
- ✅ Custom keys work (testCustomKeyClass)
- ✅ Affinity mapping (testAffinityMapping)
- ✅ Persistence config (testPersistenceConfiguration)

**Expected result:** 8/8 tests pass ✅

---

## Understanding Test Failures

### If a test fails:

1. **Read the test name** - It tells you what operation failed
2. **Check the assertion message** - Shows expected vs actual
3. **Review your lab code** - Compare with lab instructions
4. **Fix the issue** - Update your implementation
5. **Re-run the test** - Verify the fix

### Example Test Failure:

```
testPutOperation() FAILED
Expected: "value1"
Actual: null
```

**This means:** Your put operation didn't store the data correctly.

**Fix:** Check your cache.put() implementation.

---

## Common Issues and Solutions

### Issue: "Node startup failed"
**Problem:** Ignite node won't start
**Check:**
- Java 8+ installed?
- Ports 47500-47509 available?
- Configuration correct?

### Issue: "Cache not found"
**Problem:** Can't get cache reference
**Check:**
- Did you create the cache first?
- Using correct cache name?
- Called getOrCreateCache()?

### Issue: "Data not replicated"
**Problem:** Second node can't see data
**Check:**
- Backups configured (setBackups > 0)?
- Both nodes in same cluster?
- Using same cache name?

### Issue: "Metrics not available"
**Problem:** Cache metrics return 0
**Check:**
- Statistics enabled (setStatisticsEnabled(true))?
- Performed operations first?
- Using correct metrics API?

---

## Validation Workflow

### Recommended Workflow:

1. **Read Lab Instructions** → Understand requirements
2. **Write Your Code** → Implement the lab exercises
3. **Run Your Code** → Test manually
4. **Run Validation Tests** → Verify completeness
5. **Fix Any Issues** → Based on test failures
6. **Re-run Tests** → Confirm all pass
7. **Move to Next Lab** → Repeat process

### Example Session:

```bash
# After completing Lab 1
cd my-lab-solutions/lab01
mvn compile exec:java  # Run my solution

# Looks good, validate it
cd ../../tests
mvn test -Dtest=Lab01EnvironmentSetupTest

# Result: 15/16 tests passed (1 failure)
# Fix: testContainsKey failed - I forgot to implement containsKey check

# Go back and fix
cd ../my-lab-solutions/lab01
# Add containsKey implementation...

# Re-validate
cd ../../tests
mvn test -Dtest=Lab01EnvironmentSetupTest

# Result: 16/16 tests passed ✅
# Lab 1 complete! Move to Lab 2
```

---

## Running All Tests

### After completing all labs:

```bash
# Validate your entire course work
mvn clean test

# Expected result:
# Tests run: 81+
# Failures: 0
# Errors: 0
# Skipped: 0
# Success rate: 100% ✅
```

---

## Test Results Interpretation

### ✅ Green (Passing)
Your implementation matches the required operations exactly.

### ❌ Red (Failing)
Something is missing or incorrect in your implementation.

### ⚠️ Yellow (Skipped)
Test was skipped (usually shouldn't happen).

---

## Tips for Success

### 1. Test After Each Exercise
Don't wait until the end of a lab - test after each section.

### 2. Read Test Names
Test names describe exactly what they validate:
- `testPutOperation` → validates put works
- `testDataReplication` → validates data replicates
- `testCacheMetrics` → validates metrics collection

### 3. Use Test Output
Failed tests show:
- What was expected
- What you actually got
- Where the failure occurred

### 4. Run Tests Multiple Times
If a test is flaky, you might have a concurrency issue.

### 5. Check Test Implementation
Look at the test code to understand exactly what's expected.

---

## Getting Help

### If tests fail and you can't figure out why:

1. **Check lab instructions** - Did you miss a step?
2. **Review test code** - See exactly what's being tested
3. **Check common issues** - See section above
4. **Compare with lab examples** - Use provided code snippets
5. **Ask instructor** - Bring specific test failure output

---

## Test as Learning Tool

These tests aren't just validation - they're learning tools!

### Use tests to:
- **Understand requirements** - Read test code to see what's needed
- **Learn best practices** - Tests show correct usage patterns
- **Verify edge cases** - Tests check boundary conditions
- **Build confidence** - Passing tests = correct implementation

### Example: Learning from testBatchPerformance

```java
@Test
public void testBatchPerformance() {
    // Individual operations
    for (int i = 0; i < 100; i++) {
        cache.put(i, "value" + i);  // ❌ Slow
    }

    // Batch operation
    Map<Integer, String> batch = new HashMap<>();
    for (int i = 0; i < 100; i++) {
        batch.put(i, "value" + i);
    }
    cache.putAll(batch);  // ✅ Fast!
}
```

**Lesson:** Always use batch operations for multiple puts!

---

## Grading with Tests

### Instructors can use tests for grading:

```bash
# Grade student submission
cd student-submission/tests
mvn test

# Check results:
# Lab 1: 16/16 = 100%
# Lab 2: 12/14 = 86%
# Lab 3: 23/23 = 100%
# Lab 4: 18/20 = 90%
# Lab 5: 8/8 = 100%
# Overall: 77/81 = 95%
```

---

## Quick Reference

| Lab | Tests | Command |
|-----|-------|---------|
| Lab 1 | 16 | `mvn test -Dtest=Lab01*` |
| Lab 2 | 14 | `mvn test -Dtest=Lab02*` |
| Lab 3 | 23 | `mvn test -Dtest=Lab03*` |
| Lab 4 | 20 | `mvn test -Dtest=Lab04*` |
| Lab 5 | 8 | `mvn test -Dtest=Lab05*` |
| **All** | **81+** | `mvn test` |

---

## Success Criteria

You've successfully completed a lab when:

✅ All validation tests for that lab pass
✅ You understand why each test passes
✅ You can explain your implementation
✅ Your code follows best practices shown in tests

---

## Summary

This test suite is your **validation checkpoint** for each lab:

1. **Complete lab exercises** in your own code
2. **Run validation tests** to verify correctness
3. **Fix any failures** based on test output
4. **Confirm all tests pass** before moving forward
5. **Learn from test implementation** to improve understanding

**Goal:** 100% test pass rate = 100% lab completion! 🎯
