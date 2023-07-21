/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.work;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class GracefulShutdownTimeoutTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/work/graceful-shutdown-timeout-flow.xml";
  }

  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  /**
   * Dispatch an event to a service component that takes longer than default graceful shutdown time to complete and customize the
   * graceful shutdown timeout in configuration so that component execution is not interrupted. This tests services but the same
   * applies to the graceful shutdown of receivers/dispatchers etc.
   *
   * @throws Exception
   */
  @Test
  public void testGracefulShutdownTimeout() throws Exception {
    final Latch latch = new Latch();
    FlowConstruct flowConstruct = registry.<FlowConstruct>lookupByName("TestService").get();
    getFromFlow(locator, flowConstruct.getName()).setEventCallback((context, component, muleContext) -> {
      Thread.sleep(5500);
      latch.countDown();

    });

    flowRunner("TestService").withPayload("test").run();
    Thread.sleep(200);
    ((Flow) flowConstruct).dispose();
    assertTrue(latch.await(1000, MILLISECONDS));
  }
}
