#!/bin/bash

echo "🛑 Stopping Luna2 demo..."
docker compose -f docker-compose-demo.yml down -v --remove-orphans
docker container prune -f
echo "✓ Demo stopped and cleaned"
