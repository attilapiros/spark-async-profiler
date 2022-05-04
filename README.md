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

The native lib can be downloaded from the [async-profiler releases](https://github.com/jvm-profiling-tools/async-profiler/releases).

### `startCmd`

Specifies the command to start the profiler without the output file part.
Output file with the separating `,` is added by the plugin.

### `stopCmd`

Specifies the command to stop the profiler.

### `outputFileExt`

Specifies the JVM profiling output file extension. This can be ".txt" for flat traces and ".jfr"
for Java Flight Recording or ".html". the full name will contain the Spark Task ID or the Stage Id
(along with the stage attempt number) depending on the profiling mode.

The output files will be created into the temp directory prefixed with the application ID.

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
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileExt=".txt" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the outputs:

```
$ grep Creating work/app-20220504152251-0000/*/stderr
work/app-20220504152251-0000/0/stderr:22/05/04 15:22:57 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_6184387640984114410' for the JVM profiles
work/app-20220504152251-0000/1/stderr:22/05/04 15:22:57 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_16660974528309381076' for the JVM profiles
work/app-20220504152251-0000/2/stderr:22/05/04 15:22:57 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_13362700426483383128' for the JVM profiles

$ ls /var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000*
/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_13362700426483383128:
jvmprofile_TID_10.txt jvmprofile_TID_15.txt jvmprofile_TID_2.txt  jvmprofile_TID_21.txt jvmprofile_TID_9.txt

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_16660974528309381076:
jvmprofile_TID_0.txt  jvmprofile_TID_13.txt jvmprofile_TID_18.txt jvmprofile_TID_3.txt  jvmprofile_TID_7.txt
jvmprofile_TID_12.txt jvmprofile_TID_16.txt jvmprofile_TID_20.txt jvmprofile_TID_5.txt  jvmprofile_TID_8.txt

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504152251-0000_6184387640984114410:
jvmprofile_TID_1.txt  jvmprofile_TID_11.txt jvmprofile_TID_14.txt jvmprofile_TID_17.txt jvmprofile_TID_19.txt jvmprofile_TID_22.txt jvmprofile_TID_4.txt  jvmprofile_TID_6.txt
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
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileExt=".jfr" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.isStageMode="true" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the outputs:

```
$ grep "AsyncProfilerExecutorPlugin: Creating directory" work/app-20220504153254-0000/*/stderr
work/app-20220504153254-0000/0/stderr:22/05/04 15:32:59 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_13976154688256693425' for the JVM profiles
work/app-20220504153254-0000/1/stderr:22/05/04 15:32:59 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_17668837628389671720' for the JVM profiles
work/app-20220504153254-0000/2/stderr:22/05/04 15:33:00 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_14775186589805476156' for the JVM profiles

$ ls /var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000*/
/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_13976154688256693425/:
jvmprofile_stageID_11_stageAttemptNum_0.jfr jvmprofile_stageID_18_stageAttemptNum_0.jfr jvmprofile_stageID_3_stageAttemptNum_0.jfr
jvmprofile_stageID_14_stageAttemptNum_0.jfr jvmprofile_stageID_19_stageAttemptNum_0.jfr jvmprofile_stageID_4_stageAttemptNum_0.jfr
jvmprofile_stageID_15_stageAttemptNum_0.jfr jvmprofile_stageID_20_stageAttemptNum_0.jfr jvmprofile_stageID_8_stageAttemptNum_0.jfr

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_14775186589805476156/:
jvmprofile_stageID_0_stageAttemptNum_0.jfr  jvmprofile_stageID_13_stageAttemptNum_0.jfr jvmprofile_stageID_9_stageAttemptNum_0.jfr
jvmprofile_stageID_11_stageAttemptNum_0.jfr jvmprofile_stageID_1_stageAttemptNum_0.jfr

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_17668837628389671720/:
jvmprofile_stageID_10_stageAttemptNum_0.jfr jvmprofile_stageID_16_stageAttemptNum_0.jfr jvmprofile_stageID_2_stageAttemptNum_0.jfr
jvmprofile_stageID_11_stageAttemptNum_0.jfr jvmprofile_stageID_17_stageAttemptNum_0.jfr jvmprofile_stageID_5_stageAttemptNum_0.jfr
jvmprofile_stageID_12_stageAttemptNum_0.jfr jvmprofile_stageID_18_stageAttemptNum_0.jfr jvmprofile_stageID_7_stageAttemptNum_0.jfr
```

Convert JFR to flame graph:

```
$ java -cp ~/Downloads/async-profiler-2.7-macos/build/converter.jar jfr2flame /var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504153254-0000_17668837628389671720/jvmprofile_stageID_10_stageAttemptNum_0.jfr jvmprofile_stageID_10_stageAttemptNum_0.html
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
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.outputFileExt=".jfr" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.isStageMode="true" \
--conf spark.plugins.internal.conf.com.attilapiros.profiler.AsyncProfilerPlugin.filterForStages="1,17" \
examples/jars/spark-examples_2.12-3.2.1.jar
```

Find the output files:

```
$ grep "Creating" work/app-20220504151842-0000/*/stderr
work/app-20220504151842-0000/0/stderr:22/05/04 15:18:48 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_11600381988385712256' for the JVM profiles
work/app-20220504151842-0000/1/stderr:22/05/04 15:18:48 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_12894721707871964326' for the JVM profiles
work/app-20220504151842-0000/2/stderr:22/05/04 15:18:48 INFO AsyncProfilerExecutorPlugin: Creating directory '/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_16227423898856212302' for the JVM profiles

$ ls /var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-*
/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_11600381988385712256:
jvmprofile_stageID_17_stageAttemptNum_0.jfr

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_12894721707871964326:

/var/folders/s4/qrgp74ds36l9t56tx_f6w54h0000gn/T/app-20220504151842-0000_16227423898856212302:
jvmprofile_stageID_1_stageAttemptNum_0.jfr
```


### Running on YARN

To keep the JVM profile output files please set `yarn.nodemanager.delete.debug-delay-sec` to a 600 (10 minutes).

To find the directory where JVM profile output files are written please find the `profiler.AsyncProfilerExecutorPlugin: Creating directory`
executor log line on each nodes. For example on one host:

```
[user@hostname1 ~]$ hostname
hostname1.com

[user@hostname1 ~]$ yarn logs -applicationId APPLICATION-ID | grep "Starting executor\|Creating directory"
WARNING: YARN_OPTS has been replaced by HADOOP_OPTS. Using value of YARN_OPTS.
22/05/04 21:54:57 INFO client.RMProxy: Connecting to ResourceManager at hostname3.com/<IP-ADDRESS>:8032
22/05/04 21:46:08 INFO executor.Executor: Starting executor ID 1 on host hostname1.com
22/05/04 21:46:08 INFO profiler.AsyncProfilerExecutorPlugin: Creating directory '/dataroot/ycloud/yarn/nm/usercache/user/appcache/APPLICATION-ID/container_ID_000002/tmp/APPLICATION-ID_6870759781690969606' for the JVM profiles
22/05/04 21:46:14 INFO executor.Executor: Starting executor ID 2 on host hostname4.com
22/05/04 21:46:15 INFO profiler.AsyncProfilerExecutorPlugin: Creating directory '/dataroot/ycloud/yarn/nm/usercache/user/appcache/APPLICATION-ID/container_ID_000003/tmp/APPLICATION-ID_5615351009111803660' for the JVM profiles

[user@hostname1 ~]$ ls /dataroot/ycloud/yarn/nm/usercache/user/appcache/APPLICATION-ID/container_ID_000002/tmp/APPLICATION-ID_6870759781690969606
jvmprofile_stageID_1_stageAttemptNum_0.jfr
```
