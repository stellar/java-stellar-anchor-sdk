#! /bin/bash
echo "ls /config"
ls /config
cp -v /config_files/stellar_wks.toml /anchor_config
cp -v /config_files/anchor_config.yaml /anchor_config
echo "ls /anchor_config"
ls /anchor_config
echo "hello world3"
echo "tailing for eternity..."
tail -f /dev/null
    