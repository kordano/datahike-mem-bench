# Datahike memory benchmark
Monitoring the memory usage of Datahike transactions with file backend.

## Usage
Build with 
```
clj -Muberjar
```

Run with 
```
java -cp target/bench.jar clojure.main -m datahike-mem-bench.core -t 1000 -s /tmp/bench -l /tmp/log.log
```

Monitor memory usage with process `<pid>` through `jps`:
```
jstat -gc -t <pid> 500
```

## License

Copyright ©2021 Konrad Kühne

Licensed under Eclipse Public License (see LICENSE).
