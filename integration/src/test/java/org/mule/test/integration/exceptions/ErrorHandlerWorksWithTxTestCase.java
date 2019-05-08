/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.functional.api.component.EventCallback;
import org.mule.runtime.api.event.Event;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@RunnerDelegateTo(Parameterized.class)
public class ErrorHandlerWorksWithTxTestCase extends AbstractIntegrationTestCase {

  private static final String ERROR_HANDLER_REF = "errorHandlerName";
  private static List<Thread> threads = new ArrayList<>();
  private String config;

  @Parameterized.Parameters(name = "{0} - {2}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Local Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread.xml",
            "errorHandlerWithTestNonBlocking"},
        {"Local Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread.xml",
            "errorHandlerWithProcessingTypeChange"},
        {"Local Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread.xml",
            "errorHandlerWithNonBlockingOp"},
        {"Local Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread.xml",
            "errorHandlerWithNonBlockingOpAndProcessingTypeChange"},
        {"Global Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread-global-err.xml",
            "errorHandlerWithTestNonBlocking"},
        {"Global Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread-global-err.xml",
            "errorHandlerWithProcessingTypeChange"},
        {"Global Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread-global-err.xml",
            "errorHandlerWithNonBlockingOp"},
        {"Global Error Handler", "org/mule/test/integration/exceptions/error-handler-tx-same-thread-global-err.xml",
            "errorHandlerWithNonBlockingOpAndProcessingTypeChange"},
    });
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  public ErrorHandlerWorksWithTxTestCase(String type, String config, String errorHandlerName) {
    System.setProperty(ERROR_HANDLER_REF, errorHandlerName);
    this.config = config;
  }

  @After
  public void tearDown() {
    System.clearProperty(ERROR_HANDLER_REF);
    threads.clear();
  }

  @Test
  public void doesNotChangeThread() throws Exception {
    Event event = flowRunner("flowWithTx").run();

    assertThat(event.getMessage().getPayload().getValue(), is("zaraza"));
    assertThat(threads, hasSize(2));
    assertThat(threads.get(0), is(threads.get(1)));
  }

  public static class ThreadWatch implements EventCallback {

    @Override
    public void eventReceived(CoreEvent event, Object component, MuleContext muleContext) throws Exception {
      threads.add(Thread.currentThread());
    }

  }

}
