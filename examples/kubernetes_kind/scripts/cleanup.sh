#!/bin/bash
# Lab 14: Cleanup script

echo "╔════════════════════════════════════════════════════════════════╗"
echo "║   Lab 14: Cleanup                                              ║"
echo "╚════════════════════════════════════════════════════════════════╝"
echo

read -p "Delete KIND cluster 'ignite-cluster'? (y/n) " -n 1 -r
echo
if [[ $REPLY =~ ^[Yy]$ ]]; then
    echo "Deleting KIND cluster..."
    kind delete cluster --name ignite-cluster
    echo "Cleanup complete!"
else
    echo "Cleanup cancelled."
fi
