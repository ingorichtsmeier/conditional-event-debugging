package com.camunda.consulting.conditional_event_debugging;

import static org.camunda.bpm.engine.test.assertions.bpmn.BpmnAwareTests.*;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.camunda.bpm.engine.OptimisticLockingException;
import org.camunda.bpm.engine.ProcessEngine;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.camunda.community.process_test_coverage.spring_test.platform7.ProcessEngineCoverageConfiguration;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

@SpringBootTest(properties = {"camunda.bpm.job-execution.enabled=true",
    "camunda.bpm.auto-deployment-enabled=false"})
@Import(ProcessEngineCoverageConfiguration.class)
public class ConditionalEventHandlingTest {

  private static final Logger LOG = LoggerFactory.getLogger(ConditionalEventHandlingTest.class);

  @Autowired
  ProcessEngine engine;

  @Test
  public void test_send_message_to_set_variable_and_evalute_condition()
      throws InterruptedException {
    ProcessInstance processInstance = engine.getRuntimeService().startProcessInstanceByKey(
        "ConditionalEventTestProcess", Map.of("var1", 1, "correlationKey", 1));
    LOG.info("process instance started");
    
    engine.getTaskService().complete(engine.getTaskService().createTaskQuery()
        .processInstanceId(processInstance.getId()).singleResult().getId());
    
    assertThat(processInstance).isWaitingAt(findId("Value of var1 is true"));

    LOG.info("Update to 4");
    engine.getRuntimeService().createMessageCorrelation("updateVariableMessage")
        .processInstanceVariableEquals("correlationKey", 1).setVariableLocal("newValue", 4)
        .correlate();

    Thread.sleep(1000);
    assertThat(processInstance).isEnded();
  }

  @Test
  public void test_fulfil_condition_beforehead() throws InterruptedException {
    ProcessInstance processInstance = engine.getRuntimeService().startProcessInstanceByKey(
        "ConditionalEventTestProcess", Map.of("var1", 4, "correlationKey", 2));

    engine.getTaskService().complete(engine.getTaskService().createTaskQuery()
        .processInstanceId(processInstance.getId()).singleResult().getId());
    
    Thread.sleep(1500);

    assertThat(processInstance).isEnded();
  }

  @Test
  public void test_parallel_message_correlation() throws InterruptedException {
    List<ProcessInstance> alreadyRunning = engine.getRuntimeService().createProcessInstanceQuery()
        .processDefinitionKey("ConditionalEventTestProcess").list();
    ProcessInstance processInstance;
    if (alreadyRunning != null && alreadyRunning.size() > 0) {
      LOG.warn(alreadyRunning.size() + " process instances left over, don't start a new one");
      processInstance = alreadyRunning.get(0);
    } else {
      processInstance = engine.getRuntimeService().startProcessInstanceByKey(
          "ConditionalEventTestProcess", Map.of("var1", 5, "correlationKey", 3));
    }

    String userTaskId = engine.getTaskService().createTaskQuery()
        .processInstanceId(processInstance.getId()).singleResult().getId();
    
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    CompletableFuture<Void> completeUserTask = CompletableFuture.runAsync(() -> {
      LOG.info("complete the user task");
      try {
        engine.getTaskService().complete(userTaskId);
      } catch (Exception e) {
        LOG.warn("complete user task exception " + e.getLocalizedMessage());
      }
      LOG.info("user task completed");
    }, executorService);
    CompletableFuture<Void> correlateMessage4 = CompletableFuture.runAsync(() -> {
      int exceptionCounter = 0;
      boolean correlatedSuccess = false;
      while (exceptionCounter < 3 && correlatedSuccess == false) {
        LOG.info("Set newValue to 4 in round {}", exceptionCounter);
        try {
          engine.getRuntimeService().createMessageCorrelation("updateVariableMessage")
              .processInstanceVariableEquals("correlationKey", 3).setVariableLocal("newValue", 4)
              .correlate();
          correlatedSuccess = true;
          LOG.info("message 4 correlated");
        } catch (Exception e) {
          LOG.warn("value 4 " + e.getLocalizedMessage());
          if (e instanceof OptimisticLockingException) {
            LOG.warn("Optmistic locking exception");
            exceptionCounter++;
            correlatedSuccess = false;
          }
        }
      }
    }, executorService);

    CompletableFuture.allOf(completeUserTask, correlateMessage4).join();
    Thread.sleep(1000);
    assertThat(processInstance).isEnded();
  }

  @Disabled
  @Test
  public void test_parallel_message_correlationwith_diffenent_vars() throws InterruptedException {
    List<ProcessInstance> alreadyRunning = engine.getRuntimeService().createProcessInstanceQuery()
        .processDefinitionKey("ConditionalEventTestProcess").list();
    ProcessInstance processInstance;
    if (alreadyRunning != null && alreadyRunning.size() > 0) {
      LOG.warn(alreadyRunning.size() + " process instances left over, don't start a new one");
      processInstance = alreadyRunning.get(0);
    } else {
      processInstance = engine.getRuntimeService().startProcessInstanceByKey(
          "ConditionalEventTestProcess", Map.of("var1", 6, "correlationKey", 1));
    }
    ExecutorService executorService = Executors.newFixedThreadPool(3);
    CompletableFuture<Void> correlateMessage2 = CompletableFuture.runAsync(() -> {
      LOG.info("Set newValue to 2");
      try {
        engine.getRuntimeService().createMessageCorrelation("updateVariableMessage")
            .processInstanceVariableEquals("correlationKey", 1).setVariableLocal("newValue1", 2)
            .correlate();
      } catch (OptimisticLockingException e) {
        LOG.warn("value 2 " + e.getLocalizedMessage());
      }
      LOG.info("message 2 correlated");
    }, executorService);
    CompletableFuture<Void> correlateMessage3 = CompletableFuture.runAsync(() -> {
      LOG.info("Set newValue to 3");
      try {
        engine.getRuntimeService().createMessageCorrelation("updateVariableMessage")
            .processInstanceVariableEquals("correlationKey", 1).setVariableLocal("newValue2", 3)
            .correlate();
      } catch (OptimisticLockingException e) {
        LOG.warn("value 3 " + e.getLocalizedMessage());
      }
      LOG.info("message 3 correlated");
    }, executorService);
    CompletableFuture<Void> correlateMessage4 = CompletableFuture.runAsync(() -> {
      LOG.info("Set newValue to 4");
      try {
        engine.getRuntimeService().createMessageCorrelation("updateVariableMessage")
            .processInstanceVariableEquals("correlationKey", 1).setVariableLocal("newValue", 4)
            .correlate();
      } catch (OptimisticLockingException e) {
        LOG.warn("value 4 " + e.getLocalizedMessage());
      }
      LOG.info("message 4 correlated");
    }, executorService);

    CompletableFuture.allOf(correlateMessage2, correlateMessage3, correlateMessage4).join();
    Thread.sleep(1000);
    assertThat(processInstance).isEnded();
  }

}
