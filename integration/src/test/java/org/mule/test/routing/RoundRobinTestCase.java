/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.junit.Assert.assertNotNull;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.RoundRobinStory.ROUND_ROBIN;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.ArrayList;
import java.util.List;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.slf4j.Logger;

@Feature(ROUTERS)
@Story(ROUND_ROBIN)
public class RoundRobinTestCase extends AbstractIntegrationTestCase {

  private static final Logger LOGGER = getLogger(RoundRobinTestCase.class);

  private static final int NUMBER_OF_MESSAGES = 10;
  private static final int NUMBER_OF_WRITERS = 10;
  private static final int NUMBER_OF_ENDPOINTS = 5;

  private MuleClient client;

  @Override
  protected String getConfigFile() {
    return "round-robin-test.xml";
  }

  @Test
  public void testRoundRobin() throws Exception {
    client = muleContext.getClient();
    List<Thread> writers = new ArrayList<>();
    for (int i = 0; i < NUMBER_OF_WRITERS; i++) {
      writers.add(new Thread(new MessageWriter(i)));
    }
    for (Thread writer : writers) {
      writer.start();
    }
    for (Thread writer : writers) {
      writer.join();
    }

    for (int i = 0, j = 0; i < NUMBER_OF_WRITERS * NUMBER_OF_MESSAGES; i++) {
      // Message should be disrtibuted uniformly among endpoints
      String path = "test://output" + j;
      Message msg = client.request(path, 0).getRight().get();
      assertNotNull(msg);
      LOGGER.debug(path + ": " + getPayloadAsString(msg));
      j = (j + 1) % NUMBER_OF_ENDPOINTS;
    }
  }

  class MessageWriter implements Runnable {

    private int id;

    MessageWriter(int id) {
      this.id = id;
    }

    @Override
    public void run() {
      for (int i = 0; i < NUMBER_OF_MESSAGES; i++) {
        try {
          flowRunner("test-router").withPayload("Writer " + id + " Message " + i).run();
        } catch (Exception ex) {
          LOGGER.info("Unexpected exception dispatching message", ex);
        }
      }
    }
  }
}
