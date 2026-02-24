#!/bin/bash

echo "Starting tailscale funnel..."
tailscale funnel 3000 &
cd /home/merv/Developer/abantu

echo "Starting server..."
./start.sh
