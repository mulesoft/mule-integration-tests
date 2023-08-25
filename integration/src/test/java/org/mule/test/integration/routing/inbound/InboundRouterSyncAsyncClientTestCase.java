/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.routing.inbound;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import javax.inject.Inject;

import org.junit.Test;

public class InboundRouterSyncAsyncClientTestCase extends AbstractIntegrationTestCase {

  @Inject
  private TestQueueManager queueManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/inbound/inbound-router-sync-async-client-test.xml";
  }

  @Test
  public void testSync() throws Exception {
    Message result =
        flowRunner("SyncAsync").withPayload("testSync").withVariable("messageType", "sync").run().getMessage();

    assertThat(result.getPayload().getValue(), is("OK"));
  }

  @Test
  public void testAsync() throws Exception {
    flowRunner("SyncAsync").withPayload("testAsync").withVariable("messageType", "async").run();

    Message result = queueManager.read("asyncResponse", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();
    assertNotNull(result);
    assertThat(result.getPayload().getValue(), is("Response sent to asyncResponse"));
  }
}
