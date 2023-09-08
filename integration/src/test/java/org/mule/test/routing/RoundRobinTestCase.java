/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.junit.Assert.assertNotNull;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.RoundRobinStory.ROUND_ROBIN;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import org.junit.Test;
import org.slf4j.Logger;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(ROUND_ROBIN)
public class RoundRobinTestCase extends AbstractIntegrationTestCase {

  private static final Logger LOGGER = getLogger(RoundRobinTestCase.class);

  private static final int NUMBER_OF_MESSAGES = 10;
  private static final int NUMBER_OF_WRITERS = 10;
  private static final int NUMBER_OF_ENDPOINTS = 5;

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "round-robin-test.xml";
  }

  @Test
  public void testRoundRobin() throws Exception {
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
      // Message should be distributed uniformly among endpoints
      String path = "output" + j;
      Message msg = queueManager.read(path, 0, MILLISECONDS).getMessage();
      assertNotNull(msg);
      LOGGER.debug(path + ": " + getPayloadAsString(msg));
      j = (j + 1) % NUMBER_OF_ENDPOINTS;
    }
  }

  @Test
  public void testRoundRobinNonBlocking() throws Exception {
    flowRunner("test-router-nb").withPayload("AmI_NB?").runAndVerify("test-router-nb");
  }

  class MessageWriter implements Runnable {

    private final int id;

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
