#! /bin/bash
cp -v /config_files/stellar_wks.toml /anchor_config
cp -v /config_files/anchor_config.yaml /anchor_config
cp -v /config_files/reference_config.yaml /anchor_config
cp -v /config_files/sep.sh /anchor_config
cat /anchor_config/anchor_config.yaml
cat /anchor_config/stellar_wks.toml
cat /anchor_config/reference_config.yaml
echo "ls /anchor_config"
ls /anchor_config
echo "Configuration has been copied to shared volume. Exiting Config Volume"