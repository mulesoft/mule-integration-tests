/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.functional.api.component.InvocationCountMessageProcessor.getNumberOfInvocationsFor;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.RoutersFeature.AsyncStory.ASYNC;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Feature(SCOPE)
@Stories({@Story(ASYNC), @Story(ERROR_HANDLING)})
@Issue("MULE-18353")
@RunnerDelegateTo(Parameterized.class)
public class AsyncTryTestCase extends AbstractIntegrationTestCase {

  private String flowName;
  private Latch latch;

  @Parameters(name = "{0}")
  public static Collection<String> data() {
    return asList("trigger-sync",
                  "trigger-async-direct",
                  "trigger-async-flow",
                  "trigger-async-subflow",
                  "trigger-async-subflow",
                  "trigger-async-subflow",
                  "trigger-async-subflow",
                  "trigger-simplified");
  }

  public AsyncTryTestCase(String flowName) {
    this.flowName = flowName;
  }

  @Override
  protected String getConfigFile() {
    return "async-try-config.xml";
  }

  @Before
  public void setupLatch() {
    latch = new Latch();
  }

  @Test
  public void executesTheProcessorAfterTry() throws Exception {
    flowRunner(flowName).withVariable("latch", latch).run();
    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);
    assertThat(getNumberOfInvocationsFor("invocation-counter"), is(1));
  }
}
