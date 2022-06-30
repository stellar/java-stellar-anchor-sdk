#! /bin/bash
echo "ls /config"
ls /config
cp -v /config/stellar_wks.toml /anchor_config
cp -v /config/anchor_config /anchor_config
echo "ls /anchor_config"
ls /anchor_config
echo "hello world3"
echo "tailing for eternity..."
tail -f /dev/null
    