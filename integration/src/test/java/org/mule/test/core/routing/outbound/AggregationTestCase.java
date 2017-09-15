/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.routing.outbound;

import static java.util.stream.Collectors.toList;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayOutputStream;
import java.util.List;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test that aggregators preserve message order in synchronous scenarios (MULE-5998)
 */
// TODO: MULE-9303
@Ignore("MULE-9303 Review aggregator sorting using runFlow")
public class AggregationTestCase extends AbstractIntegrationTestCase {

  private static final String PAYLOAD = "Long string that will be broken up into multiple messages";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/aggregation-config.xml";
  }

  @Test
  public void testCollectionAggregator() throws Exception {
    TestConnectorQueueHandler queueHandler = new TestConnectorQueueHandler(muleContext);

    flowRunner("SplitterFlow").withPayload(PAYLOAD).run();
    Message msg = queueHandler.read("collectionCreated", RECEIVE_TIMEOUT).getMessage();
    assertNotNull(msg);
    assertTrue(msg.getPayload().getValue() instanceof List);

    List<byte[]> chunks =
        ((List<Message>) msg.getPayload().getValue()).stream()
            .map(muleMessage -> (byte[]) muleMessage.getPayload().getValue())
            .collect(toList());
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    for (byte[] chunk : chunks) {
      baos.write(chunk);
    }
    String aggregated = baos.toString();
    assertEquals(PAYLOAD, aggregated);
  }
}
