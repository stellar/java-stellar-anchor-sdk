#! /bin/bash
echo "ls /anchor_config"
ls /anchor_config
echo "ls /app"
ls /app
echo "env"
env
echo "starting anchor platform..."
cp /anchor_config/anchor_config.yaml /anchor_config/anchor-config.yaml
export _JAVA_OPTIONS=-Dlogging.level.org.springframework=DEBUG
java -jar /app/anchor-platform-runner.jar --sep-server