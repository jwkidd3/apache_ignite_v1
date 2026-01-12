#!/bin/bash
# Lab 14: Setup script for Ignite on Kubernetes with KIND

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
K8S_DIR="$SCRIPT_DIR/../kubernetes"

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   Lab 14: Apache Ignite on Kubernetes with KIND                ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo

# Check prerequisites
echo "Checking prerequisites..."

if ! command -v docker &> /dev/null; then
    echo "ERROR: Docker is not installed. Please install Docker first."
    exit 1
fi

if ! docker info &> /dev/null; then
    echo "ERROR: Docker daemon is not running. Please start Docker."
    exit 1
fi

if ! command -v kind &> /dev/null; then
    echo "KIND not found. Installing..."
    if [[ "$OSTYPE" == "darwin"* ]]; then
        brew install kind
    else
        curl -Lo ./kind https://kind.sigs.k8s.io/dl/v0.20.0/kind-linux-amd64
        chmod +x ./kind
        sudo mv ./kind /usr/local/bin/kind
    fi
fi

if ! command -v kubectl &> /dev/null; then
    echo "ERROR: kubectl is not installed. Please install kubectl first."
    exit 1
fi

echo "All prerequisites met!"
echo

# Create KIND cluster
echo "Step 1: Creating KIND cluster..."
if kind get clusters | grep -q "ignite-cluster"; then
    echo "Cluster 'ignite-cluster' already exists. Deleting..."
    kind delete cluster --name ignite-cluster
fi

kind create cluster --config "$K8S_DIR/kind-config.yaml"
echo "KIND cluster created!"
echo

# Wait for nodes to be ready
echo "Step 2: Waiting for nodes to be ready..."
kubectl wait --for=condition=Ready nodes --all --timeout=120s
echo "All nodes ready!"
echo

# Deploy Ignite
echo "Step 3: Deploying Apache Ignite..."
kubectl apply -f "$K8S_DIR/namespace.yaml"
kubectl apply -f "$K8S_DIR/ignite-configmap.yaml"
kubectl apply -f "$K8S_DIR/ignite-rbac.yaml"
kubectl apply -f "$K8S_DIR/ignite-service.yaml"
kubectl apply -f "$K8S_DIR/ignite-statefulset.yaml"
echo "Ignite resources created!"
echo

# Wait for Ignite pods
echo "Step 4: Waiting for Ignite pods to be ready (this may take 2-3 minutes)..."
kubectl wait --for=condition=Ready pod -l app=ignite -n ignite --timeout=300s
echo "Ignite cluster is ready!"
echo

# Show status
echo "═══════════════════════════════════════════════════════════════════"
echo "                         DEPLOYMENT COMPLETE"
echo "═══════════════════════════════════════════════════════════════════"
echo
echo "Ignite pods:"
kubectl get pods -n ignite
echo
echo "Services:"
kubectl get svc -n ignite
echo
echo "Access points:"
echo "  - REST API:      http://localhost:8080/ignite?cmd=version"
echo "  - Thin Client:   localhost:10800"
echo
echo "Useful commands:"
echo "  - Check topology: kubectl exec -it -n ignite ignite-0 -- /opt/ignite/apache-ignite/bin/control.sh --baseline"
echo "  - View logs:      kubectl logs -n ignite ignite-0"
echo "  - Scale cluster:  kubectl scale statefulset ignite -n ignite --replicas=5"
echo
