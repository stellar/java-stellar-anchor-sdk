#! /bin/bash
echo "hello world1"
ls /config
cp /config_files/* /anchor_config
echo "hello world2"
ls /anchor_config
echo "hello world3"
echo "tailing for eternity..."
tail -f /dev/null
