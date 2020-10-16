/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.HashSet;
import java.util.Vector;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;
import org.mule.tests.api.TestQueueManager;

@RunnerDelegateTo(Parameterized.class)
public class PayloadDecoratorThroughQueuesTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  private Object payload;

  @Parameters
  public static Collection<Object[]> data() {
    return asList(new Object[][] {{asList("cat", "cow", "dog")},
        {new HashSet(asList("cat", "cow", "dog"))},
        {new Vector(1)}});
  }

  public PayloadDecoratorThroughQueuesTestCase(Object payload) {
    this.payload = payload;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/payload-through-vm-config.xml";
  }

  @Test
  public void testPayload() throws Exception {
    sendPayload("publishConsumeThroughVM", payload);
    assertThat(queueManager.read("processed", RECEIVE_TIMEOUT, MILLISECONDS), notNullValue());
  }

  private void sendPayload(String flowName, Object payload) throws Exception {
    flowRunner(flowName)
        .withPayload(payload)
        .withMediaType(MediaType.APPLICATION_JAVA)
        .run();
  }

}
