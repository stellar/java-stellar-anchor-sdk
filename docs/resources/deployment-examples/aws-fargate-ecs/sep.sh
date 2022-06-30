#! /bin/bash
echo "ls /anchor_config"
ls /anchor_config
echo "ls /app"
ls /app
echo "env"
env
echo "starting anchor platform..."
java -jar /app/anchor-platform-runner.jar --sep-server