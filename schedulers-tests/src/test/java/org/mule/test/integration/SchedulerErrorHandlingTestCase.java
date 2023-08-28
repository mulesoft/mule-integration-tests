/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.test.AbstractSchedulerTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import org.junit.Test;

@Feature(SCHEDULER)
@Story(ERROR_HANDLING)
public class SchedulerErrorHandlingTestCase extends AbstractSchedulerTestCase {

  private static final int TIMEOUT = 3000;
  private static Latch latch = new Latch();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/scheduler-error-handling-config.xml";
  }

  @Test
  public void errorHandlerIsExecutedInSchedulerFlow() throws Exception {
    assertThat("Error handler was not executed.", latch.await(TIMEOUT, MILLISECONDS), is(true));
  }

  public static Object verifyExecution(String payload) {
    latch.release();
    return payload;
  }

}
