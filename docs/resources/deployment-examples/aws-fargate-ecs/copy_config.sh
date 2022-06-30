#! /bin/bash
echo "hello world"
ls /
ls /config
ls /config_files
cp /config_files/* /config
echo "tailing for eternity..."
tail -f /dev/null
