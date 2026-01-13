# Lab 14: Apache Ignite on Kubernetes with KIND

This bonus lab demonstrates deploying Apache Ignite on a local Kubernetes cluster using KIND (Kubernetes IN Docker).

## Prerequisites

- Docker installed and running
- kubectl CLI installed
- Java 11+ and Maven (for client application)

## Quick Start

### 1. Run Setup Script

```bash
chmod +x scripts/*.sh
./scripts/setup.sh
```

This will:
- Install KIND (if not present)
- Create a 4-node Kubernetes cluster
- Deploy Apache Ignite as a 3-node StatefulSet
- Configure services for client access

### 2. Verify Deployment

```bash
./scripts/test-cluster.sh
```

### 3. Run Java Client

```bash
mvn compile exec:java
```

### 4. Cleanup

```bash
./scripts/cleanup.sh
```

## Directory Structure

```
lab14_bonus_kubernetes_kind/
├── kubernetes/
│   ├── kind-config.yaml         # KIND cluster configuration
│   ├── namespace.yaml           # Ignite namespace
│   ├── ignite-configmap.yaml    # Ignite XML configuration
│   ├── ignite-rbac.yaml         # RBAC for K8s discovery
│   ├── ignite-service.yaml      # Headless + NodePort services
│   └── ignite-statefulset.yaml  # Ignite StatefulSet
├── scripts/
│   ├── setup.sh                 # Full setup script
│   ├── cleanup.sh               # Cleanup script
│   └── test-cluster.sh          # Test script
├── src/main/java/               # Java client application
├── pom.xml
└── README.md
```

## Access Points

| Service | URL/Port |
|---------|----------|
| REST API | http://localhost:8080 |
| Thin Client | localhost:10800 |

## Useful Commands

```bash
# View pods
kubectl get pods -n ignite

# View logs
kubectl logs -n ignite ignite-0

# Check cluster baseline
kubectl exec -it -n ignite ignite-0 -- /opt/ignite/apache-ignite/bin/control.sh --baseline

# Scale cluster
kubectl scale statefulset ignite -n ignite --replicas=5

# REST API - version
curl "http://localhost:8080/ignite?cmd=version"

# REST API - put value
curl "http://localhost:8080/ignite?cmd=put&cacheName=default-cache&key=test&val=value"

# REST API - get value
curl "http://localhost:8080/ignite?cmd=get&cacheName=default-cache&key=test"
```

## Architecture

The deployment uses:
- **StatefulSet**: Provides stable pod identities (ignite-0, ignite-1, ignite-2)
- **Headless Service**: Enables DNS-based pod discovery
- **Kubernetes IP Finder**: Automatic cluster discovery via K8s API
- **NodePort Service**: External client access via port mapping
