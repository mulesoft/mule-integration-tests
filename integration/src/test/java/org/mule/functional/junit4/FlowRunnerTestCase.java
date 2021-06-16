/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.junit4;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.reactivestreams.Publisher;

public class FlowRunnerTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException exception = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/functional/junit4/flow-runner-config.xml";
  }

  @Test
  public void flowFinishesSuccessfullyWhenExpectingException() throws Exception {
    Exception exception = flowRunner("okFailFlow").runExpectingException();
    assertNotNull(exception);
  }

  @Test
  public void flowRunFailsEvenWhenExpectingException() throws Exception {
    exception.expect(AssertionError.class);
    exception.expectMessage("evaluated false");
    final Exception exception = flowRunner("badFailFlow").runExpectingException();
    throw exception;
  }

  @Test
  @Issue("MULE-19444")
  public void applyIsCalledOnlyDuringStartup() throws Exception {
    // Apply is called when the flow is constructed.
    assertThat(TestProcessor.applyCalled, is(true));

    // Reset the flag, build the runnerZ and run the flow.
    TestProcessor.applyCalled = false;
    flowRunner("okFlow").run();

    // Apply isn't called as part of the flowRunner run() execution.
    assertThat(TestProcessor.applyCalled, is(false));
  }

  public static class TestProcessor implements Processor {

    static boolean applyCalled = false;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      return event;
    }

    @Override
    public Publisher<CoreEvent> apply(Publisher<CoreEvent> publisher) {
      applyCalled = true;
      return publisher;
    }
  }
}
