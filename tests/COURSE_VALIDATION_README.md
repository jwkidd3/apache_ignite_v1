# Course Validation Test Suite

## Purpose

This test suite validates that the **Apache Ignite training course itself is working correctly**. It ensures:

✅ All lab instructions are accurate
✅ Code examples in labs actually work
✅ Operations described in labs function correctly
✅ Course can be delivered successfully
✅ Environment setup instructions are correct
✅ No breaking changes in Ignite 2.16.0

## NOT for Students

**This test suite is NOT for students to validate their work.**

Students will write their own code following the lab instructions. This test suite is for the **instructor/course author** to verify the course materials are correct.

## Use Cases

### 1. Course Quality Assurance

Before delivering the course, run tests to verify:
```bash
mvn test
# Expected: 100% pass = Course is ready to deliver
```

### 2. Course Updates

After updating lab materials, verify nothing broke:
```bash
mvn test -Dtest=Lab03*
# Verifies Lab 3 updates didn't break anything
```

### 3. Environment Validation

Before teaching the course, verify the environment works:
```bash
mvn test -Dtest=Lab01EnvironmentSetupTest
# Verifies Ignite installs and starts correctly
```

### 4. Version Upgrades

When upgrading Ignite version, verify compatibility:
```bash
# Update Ignite version in pom.xml
mvn clean test
# Check if all operations still work
```

## What Each Test Validates

### Lab 1 Tests - Verify Environment Setup Instructions

**Validates:**
- ✅ Installation instructions work
- ✅ Node startup code examples are correct
- ✅ Basic cache operation examples work
- ✅ Configuration examples are accurate

**If tests fail:**
- Lab 1 instructions need updates
- Code examples need fixing
- Environment prerequisites missing

### Lab 2 Tests - Verify Multi-Node Clustering

**Validates:**
- ✅ Discovery configuration examples work
- ✅ Multi-node cluster formation works
- ✅ Replication examples are correct
- ✅ Backup configuration works as documented

**If tests fail:**
- Clustering instructions need clarification
- Network configuration examples need updates
- Backup examples need fixing

### Lab 3 Tests - Verify Cache Operations

**Validates:**
- ✅ All cache mode examples work
- ✅ Async operation examples are correct
- ✅ Batch operation examples function properly
- ✅ Performance claims are accurate

**If tests fail:**
- Cache configuration examples need updates
- Async code examples need fixing
- Batch operation instructions need clarification

### Lab 4 Tests - Verify Configuration Examples

**Validates:**
- ✅ Configuration code examples work
- ✅ Metrics collection examples are accurate
- ✅ Monitoring setup instructions work
- ✅ Statistics enable correctly

**If tests fail:**
- Configuration examples need updates
- Metrics API usage needs fixing
- Documentation needs clarification

### Lab 5 Tests - Verify Data Modeling

**Validates:**
- ✅ Affinity key examples work
- ✅ Colocation examples are correct
- ✅ Persistence configuration works
- ✅ Custom key examples function properly

**If tests fail:**
- Data modeling examples need updates
- Affinity key documentation needs fixing
- Persistence examples need clarification

## Running Course Validation

### Before Course Delivery

1. **Full validation:**
```bash
cd tests
mvn clean test
```

2. **Expected result:**
```
Tests run: 81+
Failures: 0
Errors: 0
Skipped: 0
Success rate: 100%
```

3. **If all pass:** ✅ Course is ready to deliver

4. **If any fail:** ❌ Fix course materials before delivery

### After Updating Lab Materials

1. **Validate affected labs:**
```bash
# Updated Lab 3
mvn test -Dtest=Lab03*

# Updated Labs 1-3
mvn test -Dtest=Lab01*,Lab02*,Lab03*
```

2. **Check results and fix materials if needed**

### When Student Reports Issue

1. **Reproduce with tests:**
```bash
# Student says Lab 2, Exercise 3 doesn't work
mvn test -Dtest=Lab02MultiNodeClusterTest#testDataReplication
```

2. **If test passes:** Issue is with student's implementation
3. **If test fails:** Issue is with course materials - fix lab instructions

## Maintenance Workflow

### Quarterly Review

```bash
# Verify course still works
mvn clean test

# Update Ignite version if needed
# Update lab materials if APIs changed
# Re-run tests
```

### Before Each Course Delivery

```bash
# Quick validation
mvn test

# If 100% pass: Ready to teach
# If any fail: Fix before class
```

### After Ignite Version Update

```bash
# Update pom.xml with new Ignite version
mvn clean test

# Review failures:
# - Update lab instructions for API changes
# - Update code examples
# - Update documentation
```

## Test Results Interpretation

### ✅ 100% Pass Rate

**Meaning:** Course materials are accurate and complete
**Action:** Ready to deliver course

### ❌ Some Tests Fail

**Meaning:** Course materials have issues
**Action:** Review failed tests and update labs

### Example Failure Analysis

```
Lab02MultiNodeClusterTest.testDataReplication FAILED
Expected: "value1"
Actual: null
```

**Diagnosis:** Replication example in Lab 2 is incorrect
**Fix:** Update Lab 2, Exercise 3 instructions
**Verify:** Re-run test after fix

## Course Quality Metrics

### Coverage

- **Lab 1:** 16 tests covering all basic operations
- **Lab 2:** 14 tests covering all clustering operations
- **Lab 3:** 23 tests covering all cache operations
- **Lab 4:** 20 tests covering all configuration operations
- **Lab 5:** 8 tests covering all data modeling operations

### Quality Indicators

| Metric | Target | Meaning |
|--------|--------|---------|
| Pass Rate | 100% | All course materials work |
| Coverage | 100% | All operations tested |
| Execution Time | <10 min | Quick validation |

## Continuous Integration

### GitHub Actions Example

```yaml
name: Course Validation

on:
  push:
    paths:
      - 'labs/**'
      - 'presentations/**'
  schedule:
    - cron: '0 0 * * 1'  # Weekly

jobs:
  validate:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up JDK 11
        uses: actions/setup-java@v2
        with:
          java-version: '11'
      - name: Validate Course Materials
        run: |
          cd tests
          mvn clean test
      - name: Report Results
        if: failure()
        run: echo "Course validation failed - review test results"
```

This ensures:
- Course materials stay current
- Breaking changes caught immediately
- Quality maintained over time

## Documentation Updates

### When Lab Instructions Change

1. Update lab markdown files
2. Run corresponding tests
3. Update tests if operations changed
4. Verify all tests pass
5. Commit changes

### When Code Examples Change

1. Update code in lab files
2. Update corresponding test if needed
3. Run tests to verify
4. Commit if all pass

## Troubleshooting

### Issue: Tests fail on fresh environment

**Cause:** Environment prerequisites not met
**Fix:**
- Verify Java 11+ installed
- Check port availability (47500-47509)
- Ensure sufficient memory (4GB+)
- Update lab prerequisites documentation

### Issue: Tests pass locally but fail in CI

**Cause:** Environment differences
**Fix:**
- Check CI Java version
- Verify CI memory allocation
- Check network configuration
- Update CI pipeline config

### Issue: Single test fails consistently

**Cause:** Lab instruction error
**Fix:**
- Review specific lab section
- Test code example manually
- Update lab instructions
- Update test if needed

## Summary

This test suite is a **quality assurance tool** for the Apache Ignite training course:

🎯 **Purpose:** Validate course materials work correctly
🎯 **Users:** Instructors, course authors, QA
🎯 **Usage:** Before delivery, after updates, continuous validation
🎯 **Goal:** Ensure 100% accurate course materials

**NOT for student work validation** - students follow labs independently.

---

## Quick Reference

| Action | Command | Purpose |
|--------|---------|---------|
| Validate entire course | `mvn test` | Before delivery |
| Validate single lab | `mvn test -Dtest=Lab01*` | After lab update |
| Quick check | `mvn test -Dtest=Lab01EnvironmentSetupTest#testNodeStartup` | Verify specific operation |
| Generate report | `mvn test jacoco:report` | Coverage analysis |

**Course validation status:** Run `mvn test` → 100% pass = Ready to deliver ✅
