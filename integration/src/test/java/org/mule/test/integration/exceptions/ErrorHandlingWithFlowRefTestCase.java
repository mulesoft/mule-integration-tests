/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Issue;

@Issue("MULE-16892")
public class ErrorHandlingWithFlowRefTestCase extends AbstractIntegrationTestCase {

  private static int executions;

  @Before
  public void resetExecutions() {
    executions = 0;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler-flow-ref.xml";
  }

  @Test
  public void errorHandlerExecutedOnceWithAllStaticFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-all-static").run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithDynamicFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-dynamic")
        .withVariable("flowToExecute", "flow-modif")
        .run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithDynamicErrorFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-error-dynamic")
        .withVariable("errorFlowToExecute", "flow-with-error")
        .run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithAllDynamicFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-error-dynamic")
        .withVariable("errorFlowToExecute", "flow-with-error")
        .withVariable("flowToExecute", "flow-modif")
        .run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithAllStaticFlowRefSubFlow() throws Exception {
    flowRunner("error-handler-with-flow-ref-to-sub-flow").run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithAllStaticFlowRefAllSubFlow() throws Exception {
    flowRunner("error-handler-with-flow-ref-all-sub-flow").run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithAllStaticFlowRefErrorSubFlow() throws Exception {
    flowRunner("error-handler-with-flow-ref-to-error-sub-flow").run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithDynamicSubFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-dynamic")
        .withVariable("flowToExecute", "sub-flow-modif")
        .run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithDynamicErrorSubFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-error-dynamic")
        .withVariable("errorFlowToExecute", "sub-flow-with-error")
        .run();
    assertThat(executions, is(1));
  }

  @Test
  public void errorHandlerExecutedOnceWithAllDynamicSubFlowRef() throws Exception {
    flowRunner("error-handler-with-flow-ref-error-dynamic")
        .withVariable("errorFlowToExecute", "sub-flow-with-error")
        .withVariable("flowToExecute", "sub-flow-modif")
        .run();
    assertThat(executions, is(1));
  }

  public static class TestProcessorCounter extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      executions++;
      return event;
    }

  }
}
