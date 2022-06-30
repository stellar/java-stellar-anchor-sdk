#! /bin/bash
echo "hello world1"
ls /config
cp /config_files/* /config
echo "hello world2"
ls /config
echo "hello world3"
echo "tailing for eternity..."
tail -f /dev/null
