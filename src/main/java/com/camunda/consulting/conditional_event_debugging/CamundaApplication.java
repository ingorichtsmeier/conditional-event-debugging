package com.camunda.consulting.conditional_event_debugging;

import org.camunda.bpm.spring.boot.starter.annotation.EnableProcessApplication;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
@EnableProcessApplication("conditional-event-debugging")
public class CamundaApplication {

  public static void main(String... args) {
    SpringApplication.run(CamundaApplication.class, args);
  }

}
