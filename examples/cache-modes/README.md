# Cache Modes Example

Demonstrates Apache Ignite cache modes: PARTITIONED, REPLICATED, and PARTITIONED with no backups.

## Files

- **CacheModesServer.java** - Creates caches and populates data (server node)
- **CacheModesClient.java** - Joins cluster as client node and reads data

## Usage

1. Build the project:
   ```bash
   mvn compile
   ```

2. Start a server node (keeps running):
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.CacheModesServer"
   ```

3. (Optional) Start additional server nodes in separate terminals:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.CacheModesServer" -Dexec.args="2"
   ```

4. Run the client to read data:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.CacheModesClient"
   ```

## Cache Modes

| Mode | Description | Use Case |
|------|-------------|----------|
| PARTITIONED (1 backup) | Data distributed across nodes with redundancy | Large datasets, horizontal scaling |
| REPLICATED | Full copy on every node | Small, read-heavy reference data |
| PARTITIONED (0 backups) | Distributed, no redundancy | Temporary/recoverable data only |
