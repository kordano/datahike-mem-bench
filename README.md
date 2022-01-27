# Datahike memory benchmark
Monitoring the memory usage of Datahike transactions with file backend.

## Usage
Build with 
```
clj -Muberjar
```

Run with 
```
clj -J-XX:+UseG1GC -J-XX:InitialHeapSize=2g -J-XX:MaxHeapSize=4g -J-XX:+UseStringDeduplication -J-XX:MaxTenuringThreshold=1 -J-Xlog:gc=debug:file=/root/datahike-mem-bench/gc.log:time,uptime,level,tags:filecount=5,filesize=100m -m datahike-mem-bench.core -c /root/datahike-mem-bench/pg_config.edn -t 1000000 -l /root/datahike-mem-bench/pg_log.out
```

## License

Copyright ©2022 Konrad Kühne

Licensed under Eclipse Public License (see LICENSE).
