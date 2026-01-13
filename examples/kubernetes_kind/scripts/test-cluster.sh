#!/bin/bash
# Lab 14: Test script for Ignite cluster on Kubernetes

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   Lab 14: Testing Ignite Cluster on Kubernetes                 ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo

# Check cluster status
echo "=== Cluster Nodes ==="
kubectl get nodes
echo

echo "=== Ignite Pods ==="
kubectl get pods -n ignite -o wide
echo

echo "=== Ignite Services ==="
kubectl get svc -n ignite
echo

# Test REST API
echo "=== Testing REST API ==="
echo "GET version..."
curl -s "http://localhost:8080/ignite?cmd=version" | head -c 200
echo
echo

# Test cache operations
echo "=== Testing Cache Operations ==="
echo "PUT test value..."
curl -s "http://localhost:8080/ignite?cmd=put&cacheName=default-cache&key=k8s-test&val=HelloFromK8s"
echo

echo "GET test value..."
curl -s "http://localhost:8080/ignite?cmd=get&cacheName=default-cache&key=k8s-test"
echo
echo

# Show baseline
echo "=== Cluster Baseline ==="
kubectl exec -it -n ignite ignite-0 -- /opt/ignite/apache-ignite/bin/control.sh --baseline 2>/dev/null || echo "Could not get baseline"
echo

echo "Tests complete!"
