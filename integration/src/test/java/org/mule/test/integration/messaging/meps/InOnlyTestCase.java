/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.messaging.meps;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertTrue;

import org.mule.functional.api.notification.FunctionalTestNotificationListener;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class InOnlyTestCase extends AbstractIntegrationTestCase {

  public static final long TIMEOUT = 3000;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/pattern_In-Only-flow.xml";
  }

  @Test
  public void testExchange() throws Exception {
    final Latch latch = new Latch();
    notificationListenerRegistry.registerListener((FunctionalTestNotificationListener) notification -> latch.countDown());

    flowRunner("In-Only-Service").withPayload(TEST_PAYLOAD).run();
    assertTrue(latch.await(TIMEOUT, MILLISECONDS));
  }
}
