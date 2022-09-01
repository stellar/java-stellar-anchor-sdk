#!/bin/bash
cp -rf /config_defaults/* /config

# replace default files in /config with override files
if [ -d "/config_override" ];
  then
    cp -rf /config_override/* /config;
fi

java -jar /app/anchor-platform-runner.jar $1