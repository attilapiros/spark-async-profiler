# spark-async-profiler

Spark3 plugin to integrate with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) with the capability run the profiler for each tasks / stages separately.

## Prerequisite for building the plugin

As long `async-profiler` is not relased into an offical maven repo you should check it out yourself and build it from source:

```
$ git clone git@github.com:jvm-profiling-tools/async-profiler.git
$ mvn install -DskipTests -Dgpg.skip
```

## Example usage


### Flat stack traces for each tasks

Run the profiler:

```
./bin/spark-submit --master "local-cluster[3,1,1200]" \
--class org.apache.spark.examples.sql.SparkSQLExample \
--jars "/Users/attilazsoltpiros/.m2/repository/com/attilapiros/spark-async-profiler/1.0-SNAPSHOT/spark-async-profiler-1.0-SNAPSHOT-jar-with-dependencies.jar" \
--files "/Users/attilazsoltpiros/Downloads/async-profiler-2.7-macos/build/libasyncProfiler.so" \
--conf spark.plugins="com.attilapiros.profiler.AsyncProfilerPlugin"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.nativeLibPath="libasyncProfiler.so" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.startCmd="start,event=cpu,flat=20"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.stopCmd="stop" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileTemplate="traces_flat_%s.txt" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the outputs:

```
➜  spark-3.2.1-bin-hadoop3.2 find . -name "traces*"
./work/app-20220502153737-0000/0/traces_flat_TID_21.txt
./work/app-20220502153737-0000/0/traces_flat_TID_20.txt
./work/app-20220502153737-0000/0/traces_flat_TID_22.txt
./work/app-20220502153737-0000/0/traces_flat_TID_18.txt
./work/app-20220502153737-0000/0/traces_flat_TID_8.txt
./work/app-20220502153737-0000/0/traces_flat_TID_5.txt
./work/app-20220502153737-0000/0/traces_flat_TID_4.txt
./work/app-20220502153737-0000/0/traces_flat_TID_17.txt
./work/app-20220502153737-0000/0/traces_flat_TID_2.txt
./work/app-20220502153737-0000/0/traces_flat_TID_11.txt
./work/app-20220502153737-0000/0/traces_flat_TID_0.txt
./work/app-20220502153737-0000/1/traces_flat_TID_19.txt
./work/app-20220502153737-0000/1/traces_flat_TID_3.txt
./work/app-20220502153737-0000/1/traces_flat_TID_12.txt
./work/app-20220502153737-0000/1/traces_flat_TID_13.txt
./work/app-20220502153737-0000/2/traces_flat_TID_9.txt
./work/app-20220502153737-0000/2/traces_flat_TID_14.txt
./work/app-20220502153737-0000/2/traces_flat_TID_15.txt
./work/app-20220502153737-0000/2/traces_flat_TID_6.txt
./work/app-20220502153737-0000/2/traces_flat_TID_16.txt
./work/app-20220502153737-0000/2/traces_flat_TID_7.txt
./work/app-20220502153737-0000/2/traces_flat_TID_1.txt
./work/app-20220502153737-0000/2/traces_flat_TID_10.txt
```

### Produce JFR for each stage

Run with profiler:

```
./bin/spark-submit --master "local-cluster[3,1,1200]" \
--class org.apache.spark.examples.sql.SparkSQLExample \
--jars "/Users/attilazsoltpiros/.m2/repository/com/attilapiros/spark-async-profiler/1.0-SNAPSHOT/spark-async-profiler-1.0-SNAPSHOT-jar-with-dependencies.jar" \
--files "/Users/attilazsoltpiros/Downloads/async-profiler-2.7-macos/build/libasyncProfiler.so" \
--conf spark.plugins="com.attilapiros.profiler.AsyncProfilerPlugin"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.nativeLibPath="libasyncProfiler.so" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.startCmd="start,event=cpu,jfr"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.stopCmd="stop" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileTemplate="traces_%s.jfr" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.isStageMode="true" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the outputs:

```
$ find . -name "*.jfr"
./work/app-20220502154257-0000/0/traces_stageID_0_stageAttemptNum_0.jfr
./work/app-20220502154257-0000/1/traces_stageID_1_stageAttemptNum_0.jfr
./work/app-20220502154257-0000/2/traces_stageID_3_stageAttemptNum_0.jfr
```

Convert JFR to flame graph:

```
$ java -cp ~/Downloads/async-profiler-2.7-macos/build/converter.jar jfr2flame  ./work/app-20220502154257-0000/1/traces_stageID_1_stageAttemptNum_0.jfr traces_stageID_1_stageAttemptNum_0.html
```
