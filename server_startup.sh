#!/bin/bash

echo "Starting tailscale funnel..."
tailscale funnel 3000 &
cd /home/merv/Developer/source-be

echo "Starting server..."
./merv_start.sh
