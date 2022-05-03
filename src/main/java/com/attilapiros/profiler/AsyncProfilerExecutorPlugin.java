package com.attilapiros.profiler;

import java.io.IOException;
import java.io.File;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Map;
import java.util.HashSet;
import java.util.Set;

import org.apache.spark.api.plugin.ExecutorPlugin;
import org.apache.spark.api.plugin.PluginContext;
import org.apache.spark.TaskFailedReason;
import org.apache.spark.TaskContext;

import one.profiler.AsyncProfiler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

interface ParamKeys {

  // path of libasyncProfiler.so
  String NATIVE_LIB_PATH = "nativeLibPath";

  // optional value if empty not used at all! like: "/tmp/asyncProfiler/profile_%s.jfr" where
  // %d will be the actual taskAttemptId, if relative path is given then filew will be stored in the executors local directories
  String OUTPUTFILE_TEMPLATE = "outputFileTemplate";

  // example: "start,event=cpu"
  String PROFILE_START_COMMAND = "startCmd";

  // example: "stop"
  String PROFILE_STOP_COMMAND = "stopCmd";

  // either true or false
  String STAGE_MODE = "isStageMode";

  // a numbers sepparated by comas
  String FILTER_FOR_STAGES = "filterForStages";
}

abstract class ProfilerStrategy {

  private static final Logger logger = LoggerFactory.getLogger(ProfilerStrategy.class);

  protected AsyncProfiler asyncProfiler;

  protected String startCmd;

  protected String stopCmd;

  protected String outputFileTemplate;

  protected Set<Integer> filterForStages;

  protected ProfilerStrategy(AsyncProfiler asyncProfiler, String startCmd, String stopCmd, String outputFileTemplate, Set<Integer> filterForStages) {
    this.asyncProfiler = asyncProfiler;
    this.startCmd = startCmd;
    this.stopCmd = stopCmd;
    this.outputFileTemplate = outputFileTemplate;
    this.filterForStages = filterForStages;
  }

  public abstract void onTaskStart();

  public abstract void onTaskFinished();

  public abstract void shutdown();

  protected void stopProfilerWriteRecording(String formattedOutputFile) {
    try {
      final String fullStopCmd;
      fullStopCmd = stopCmd + formattedOutputFile;
      logger.info("fullStopCmd: {}", fullStopCmd);
      asyncProfiler.execute(fullStopCmd);
    } catch (IOException ie) {
    }
  }
}


class TaskLevelProfiler extends ProfilerStrategy {

  private static final Logger logger = LoggerFactory.getLogger(TaskLevelProfiler.class);

  private String formattedOutputFile;

  public TaskLevelProfiler(AsyncProfiler asyncProfiler, String startCmd, String stopCmd, String outputFileTemplate, Set<Integer> filterForStages) {
    super(asyncProfiler, startCmd, stopCmd, outputFileTemplate, filterForStages);
  }

  @Override
  public void onTaskStart() {
    if (formattedOutputFile != null) {
      logger.error("For AsyncProfilerExecutorPlugin executor-cores must be 1!");
    } else {
      TaskContext taskContext = TaskContext.get();
      if (filterForStages.isEmpty() || filterForStages.contains(taskContext.stageId())) {
        try {
          if (outputFileTemplate.isEmpty()) {
            formattedOutputFile = "";
          } else {
            formattedOutputFile = ",file=" + String.format(outputFileTemplate, "TID_" + taskContext.taskAttemptId());
          }
          String fullStartCmd = startCmd + formattedOutputFile;
          logger.info("fullStartCmd: {}", fullStartCmd);
          asyncProfiler.execute(fullStartCmd);
        } catch (IOException ie) {
          formattedOutputFile = null;
        }
      }
    }
  }

  @Override
  public synchronized void onTaskFinished() {
    if (formattedOutputFile != null) {
      stopProfilerWriteRecording(formattedOutputFile);
      formattedOutputFile = null;
    }
  }

  @Override
  public void shutdown() { }
}

class StageLevelProfiler extends ProfilerStrategy {

  private static final Logger logger = LoggerFactory.getLogger(StageLevelProfiler.class);

  private String formattedOutputFile;

  private int stageId = -1;

  private int stageAttemptNumber = -1;

  public StageLevelProfiler(AsyncProfiler asyncProfiler, String startCmd, String stopCmd, String outputFileTemplate, Set<Integer> filterForStages) {
    super(asyncProfiler, startCmd, stopCmd, outputFileTemplate, filterForStages);
  }

  @Override
  public void onTaskStart() {
      TaskContext taskContext = TaskContext.get();
      if (stageId != taskContext.stageId() || stageAttemptNumber != taskContext.stageAttemptNumber()) {
        if (stageId != -1) {
          stopProfilerWriteRecording(formattedOutputFile);
          formattedOutputFile = null;
        }
        if (filterForStages.isEmpty() || filterForStages.contains(taskContext.stageId())) {
          try {
            if (outputFileTemplate.isEmpty()) {
              formattedOutputFile = "";
            } else {
              formattedOutputFile = ",file=" + String.format(outputFileTemplate, "stageID_" + taskContext.stageId() + "_stageAttemptNum_" + taskContext.stageAttemptNumber());
            }
            String fullStartCmd = startCmd + formattedOutputFile;
            logger.info("fullStartCmd: {}", fullStartCmd);
            asyncProfiler.execute(fullStartCmd);
            stageId = taskContext.stageId();
            stageAttemptNumber = taskContext.stageAttemptNumber();
          } catch (IOException ie) {
            formattedOutputFile = null;
            stageId = -1;
            stageAttemptNumber = -1;
          }
        }
    }
  }

  @Override
  public synchronized void onTaskFinished() {
  }

  @Override
  public void shutdown() {
    if (formattedOutputFile != null) {
      stopProfilerWriteRecording(formattedOutputFile);
      formattedOutputFile = null;
    }
  }
}

public class AsyncProfilerExecutorPlugin implements ExecutorPlugin {
  private static final Logger logger = LoggerFactory.getLogger(AsyncProfilerExecutorPlugin.class);

  private ProfilerStrategy profilerStrategy;

  @Override
  public void init(PluginContext ctx, java.util.Map<String,String> extraConf) {
    logger.info("AsyncProfilerExecutorPlugin initialization with extraConf: " + extraConf);
    try {
      final String nativeLibPath = extraConf.getOrDefault(ParamKeys.NATIVE_LIB_PATH, "libasyncProfiler.so");
      final String fullNativeLibPath;
      final File f = new File(nativeLibPath);
      if (f.exists()) {
        fullNativeLibPath = f.getAbsolutePath();
      } else {
        fullNativeLibPath = Paths.get(".").resolveSibling(nativeLibPath).toAbsolutePath().toString();
      }
      AsyncProfiler asyncProfiler = AsyncProfiler.getInstance(fullNativeLibPath);
      String startCmd = extraConf.getOrDefault(ParamKeys.PROFILE_START_COMMAND, "start,event=cpu");
      String stopCmd = extraConf.getOrDefault(ParamKeys.PROFILE_STOP_COMMAND, "stop");
      String outputFileTemplate = extraConf.getOrDefault(ParamKeys.OUTPUTFILE_TEMPLATE, "/tmp/asyncProfiler/profile_%s.jfr");
      String filterForStagesOneStr = extraConf.getOrDefault(ParamKeys.FILTER_FOR_STAGES, "");
      final Set<Integer> filterForStages;
      if (filterForStagesOneStr.isEmpty()) {
        filterForStages = new HashSet<Integer>();
      } else {
        filterForStages = new HashSet<Integer>(Arrays.stream(filterForStagesOneStr.split(",")).mapToInt(Integer::parseInt).collect(ArrayList::new,ArrayList::add,ArrayList::addAll));
      }
      logger.info("filterForStages: " + filterForStages);
      if (Boolean.parseBoolean(extraConf.getOrDefault(ParamKeys.STAGE_MODE, "false"))) {
        profilerStrategy = new StageLevelProfiler(asyncProfiler, startCmd, stopCmd, outputFileTemplate, filterForStages);
      } else {
        profilerStrategy = new TaskLevelProfiler(asyncProfiler, startCmd, stopCmd, outputFileTemplate, filterForStages);
      }
    } catch (Throwable t) {
      logger.error("exception during AsyncProfilerExecutorPlugin initialization", t);
    }
  }

  @Override
  public void shutdown() {
    profilerStrategy.shutdown();
  }

  @Override
  public synchronized void onTaskStart() {
    profilerStrategy.onTaskStart();
  }

  @Override
  public synchronized void onTaskSucceeded() {
    profilerStrategy.onTaskFinished();
  }

  @Override
  public synchronized void onTaskFailed(TaskFailedReason failureReason) {
    profilerStrategy.onTaskFinished();
  }

}
