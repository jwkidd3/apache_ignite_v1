# Peer Class Loading Example

Demonstrates Peer Class Loading for dynamic code deployment in Apache Ignite.

## What is Peer Class Loading?

Peer class loading allows you to deploy compute task classes to remote nodes automatically. The server nodes don't need to have the task classes in their classpath - they receive the bytecode from the client and load the classes dynamically.

## Files

- **PeerClassLoadingServer.java** - Server with peer class loading enabled
- **PeerClassLoadingClient.java** - Client that sends compute tasks

## Usage

1. Build the project:
   ```bash
   mvn compile
   ```

2. Start the server:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.PeerClassLoadingServer"
   ```

3. In another terminal, run the client:
   ```bash
   mvn exec:java -Dexec.mainClass="com.example.ignite.PeerClassLoadingClient"
   ```

## Key Configuration

### Enable Peer Class Loading

```java
IgniteConfiguration cfg = new IgniteConfiguration();
cfg.setPeerClassLoadingEnabled(true);
cfg.setDeploymentMode(DeploymentMode.SHARED);
```

### Deployment Modes

| Mode | Description |
|------|-------------|
| PRIVATE | Each node loads classes independently (default) |
| ISOLATED | Each task gets its own class loader |
| SHARED | Classes are shared across all tasks |
| CONTINUOUS | Classes persist even after originating node leaves |

## What the Example Demonstrates

1. **Broadcast Runnable** - Send code to execute on all server nodes
2. **Callable with Result** - Execute code and return results
3. **Distributed Calculation** - Run multiple calculations in parallel
4. **Custom Task Classes** - Deploy arbitrary task classes dynamically

## How It Works

```
Client                          Server
  |                               |
  |  1. Send task bytecode  ----> |
  |                               |  2. Load class dynamically
  |                               |  3. Execute task
  |  <---- 4. Return result       |
```

## Benefits

- **No manual deployment** - Classes automatically transferred
- **Dynamic updates** - Change code without restarting cluster
- **Rapid development** - Test changes immediately
- **Simplified operations** - No need to sync JARs across nodes

## Limitations

- **Performance overhead** - Class transfer adds latency
- **Security considerations** - Arbitrary code execution
- **Not for production** - Use explicit deployment in production
- **Serialization required** - Classes must be serializable

## Best Practices

1. **Development only** - Use peer class loading during development
2. **Production deployment** - Deploy JARs explicitly in production
3. **Security** - Only enable in trusted environments
4. **Version control** - Be careful with class version mismatches

## Production Alternative

For production, deploy classes explicitly:

```bash
# Copy JAR to all nodes
scp my-tasks.jar user@node1:/opt/ignite/libs/
scp my-tasks.jar user@node2:/opt/ignite/libs/
scp my-tasks.jar user@node3:/opt/ignite/libs/
```

Then disable peer class loading:
```java
cfg.setPeerClassLoadingEnabled(false);
```
