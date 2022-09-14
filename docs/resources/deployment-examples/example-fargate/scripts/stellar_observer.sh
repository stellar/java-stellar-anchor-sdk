#! /bin/bash
echo "ls /anchor_config"
cat /anchor_config/anchor_config.yaml
cat /anchor_config/stellar_wks.toml
echo "ls /app"
ls /app
echo "env"
env
echo "starting stellar observer..."
export _JAVA_OPTIONS=-Dlogging.level.org.springframework=DEBUG
java -jar /app/anchor-platform-runner.jar --stellar-observer