/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
