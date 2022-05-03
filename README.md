# spark-async-profiler

A Spark 3 plugin to integrate with [async-profiler](https://github.com/jvm-profiling-tools/async-profiler) with the capability run the profiler for each tasks / stages separately.

## Prerequisite for building the plugin

As long `async-profiler` is not released into an official maven repo you should check it out yourself and build it from source:

```
$ git clone git@github.com:jvm-profiling-tools/async-profiler.git
$ mvn install -DskipTests -Dgpg.skip
```

## Plugin parameters

Common prefix: `spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin`.


### `nativeLibPath`

Specifies the location of the `libasyncProfiler.so` which is the profiler implementation.
Its default is `libasyncProfiler.so` so when `libasyncProfiler.so` is added with the `--files`
this parameter can be omitted.

### `startCmd`

Specifies the command to start the profiler without the output file part.
Output file with the separating `,` is added by using the
`spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileTemplate`
parameter (see later).

### `stopCmd`

Specifies the command to stop the profiler.

### `outputFileTemplate`

Specifies the output file template which should contain a `%s` part where depending on profiling mode
either the Spark Task ID or the Stage Id along with the attempt number will be added.

### `isStageMode`

Specifies the profiling mode.
When this is `true` then only stages are profiled separately and not the tasks.
Its default is `false`.

### `filterForStages`

Specifies the stages where the profiling is active for both stage level and task level profiling.

## Example usage

### Flat stack traces for each tasks

Run the profiler:

```
./bin/spark-submit --master "local-cluster[3,1,1200]" \
--class org.apache.spark.examples.sql.SparkSQLExample \
--jars "/Users/attilazsoltpiros/.m2/repository/com/attilapiros/spark-async-profiler/1.0-SNAPSHOT/spark-async-profiler-1.0-SNAPSHOT-jar-with-dependencies.jar" \
--files "/Users/attilazsoltpiros/Downloads/async-profiler-2.7-macos/build/libasyncProfiler.so" \
--conf spark.plugins="com.attilapiros.profiler.AsyncProfilerPlugin"  \
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
./work/app-20220502210219-0000/0/traces_flat_stageID_1_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/0/traces_flat_stageID_11_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/0/traces_flat_stageID_5_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/0/traces_flat_stageID_10_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/0/traces_flat_stageID_18_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/0/traces_flat_stageID_4_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_15_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_3_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_17_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_9_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_11_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_0_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/1/traces_flat_stageID_8_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_17_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_7_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_19_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_11_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_13_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_2_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_14_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_16_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_18_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_12_stageAttemptNum_0.jfr
./work/app-20220502210219-0000/2/traces_flat_stageID_20_stageAttemptNum_0.jfr
```

Convert JFR to flame graph:

```
$ java -cp ~/Downloads/async-profiler-2.7-macos/build/converter.jar jfr2flame  ./work/app-20220502154257-0000/1/traces_stageID_1_stageAttemptNum_0.jfr traces_stageID_1_stageAttemptNum_0.html
```


### Stage filtering

Run the profiler:

```
$ ./bin/spark-submit --master "local-cluster[3,1,1200]" \
--class org.apache.spark.examples.sql.SparkSQLExample \
--jars "/Users/attilazsoltpiros/.m2/repository/com/attilapiros/spark-async-profiler/1.0-SNAPSHOT/spark-async-profiler-1.0-SNAPSHOT-jar-with-dependencies.jar" \
--files "/Users/attilazsoltpiros/Downloads/async-profiler-2.7-macos/build/libasyncProfiler.so" \
--conf spark.plugins="com.attilapiros.profiler.AsyncProfilerPlugin"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.startCmd="start,event=cpu,jfr"  \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.stopCmd="stop" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileTemplate="traces_flat_%s.jfr" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.isStageMode="true" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.filterForStages="1,17" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the output files:

```
$ find . -name  "*.jfr"
./work/app-20220502211043-0000/1/traces_flat_stageID_1_stageAttemptNum_0.jfr
./work/app-20220502211043-0000/2/traces_flat_stageID_17_stageAttemptNum_0.jfr
```


## TODO

- Run it on YARN!
