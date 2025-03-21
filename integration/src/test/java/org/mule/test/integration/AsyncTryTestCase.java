/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ScopeFeature.SCOPE;
import static org.mule.test.allure.AllureConstants.ScopeFeature.AsyncStory.ASYNC;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

import java.util.Collection;

import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(SCOPE)
@Stories({@Story(ASYNC), @Story(ERROR_HANDLING)})
@Issue("MULE-18353")
@RunnerDelegateTo(Parameterized.class)
public class AsyncTryTestCase extends AbstractIntegrationTestCase {

  private final String flowName;
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

  @Inject
  private TestQueueManager queueManager;

  public AsyncTryTestCase(String flowName) {
    this.flowName = flowName;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/async-try-config.xml";
  }

  @Before
  public void setupLatch() {
    latch = new Latch();
  }

  @Test
  public void executesTheProcessorAfterTry() throws Exception {
    flowRunner(flowName).withVariable("latch", latch).run();
    latch.await(RECEIVE_TIMEOUT, MILLISECONDS);

    assertThat(queueManager.read("invocationQueue", RECEIVE_TIMEOUT, MILLISECONDS), not(nullValue()));;
  }
}
