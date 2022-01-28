#!/bin/bash
clj -J-XX:+UseG1GC -J-XX:InitialHeapSize=2g -J-XX:MaxHeapSize=4g -J-XX:+UseStringDeduplication -J-XX:MaxTenuringThreshold=1 -J-Xlog:gc=debug:file=/root/datahike-mem-bench/gc.log:time,uptime,level,tags:filecount=5,filesize=100m -m datahike-mem-bench.core -c /root/datahike-mem-bench/config.edn -t 1000000 -l /root/datahike-mem-bench/log.out -x 20 -u 100
