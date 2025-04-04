package com.camunda.consulting.conditional_event_debugging;

import org.camunda.bpm.engine.delegate.DelegateExecution;
import org.camunda.bpm.engine.delegate.JavaDelegate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class UpdateVariableDelegate implements JavaDelegate {
    
  private static final Logger LOG = LoggerFactory.getLogger(UpdateVariableDelegate.class);

  @Override
  public void execute(DelegateExecution execution) throws Exception {
    LOG.info("Execute UpdateVariableDelegate");
    int newValue = (int) execution.getVariable("newValue");
    execution.setVariable("var1", newValue);
    LOG.info("Updated to {}", newValue);
  }

}
