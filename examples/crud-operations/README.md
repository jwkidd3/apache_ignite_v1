# CRUD Operations Example

Demonstrates basic cache operations in Apache Ignite: put, get, remove, replace, and bulk operations.

## Files

- **CrudOperationsExample.java** - Demonstrates all CRUD operations

## Usage

1. Build the project:
   ```bash
   mvn compile
   ```

2. Run the example:
   ```bash
   mvn exec:java
   ```

## Operations Covered

| Operation | Description |
|-----------|-------------|
| `put` / `get` | Basic create/read |
| `putAll` / `getAll` | Bulk operations |
| `putIfAbsent` | Create only if key doesn't exist |
| `replace` | Update only if key exists |
| `remove` | Delete entry |
| `getAndPut` | Returns old value while updating |
| `getAndRemove` | Returns value while deleting |
| `containsKey` | Check if key exists |
| `clear` | Remove all entries |
