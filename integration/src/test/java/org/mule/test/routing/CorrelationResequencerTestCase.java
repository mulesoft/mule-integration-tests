/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.api.component.FunctionalTestProcessor.getFromFlow;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.tck.junit4.matcher.EventMatcher.hasMessage;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class CorrelationResequencerTestCase extends AbstractIntegrationTestCase {

  private CountDownLatch receiveLatch = new CountDownLatch(6);

  @Override
  protected String getConfigFile() {
    return "correlation-resequencer-test-flow.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();

    getFromFlow(locator, "sorted").setEventCallback((context, component, muleContext) -> receiveLatch.countDown());
  }

  @Test
  public void testResequencer() throws Exception {
    flowRunner("splitter").withPayload(asList("a", "b", "c", "d", "e", "f")).run();

    FunctionalTestProcessor resequencer = getFromFlow(locator, "sorted");

    assertTrue(receiveLatch.await(3000, TimeUnit.SECONDS));

    assertEquals("Wrong number of messages received.", 6, resequencer.getReceivedMessagesCount());
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(1), hasMessage(hasPayload(is("a"))));
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(2), hasMessage(hasPayload(is("b"))));
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(3), hasMessage(hasPayload(is("c"))));
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(4), hasMessage(hasPayload(is("d"))));
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(5), hasMessage(hasPayload(is("e"))));
    assertThat("Sequence wasn't reordered.", resequencer.getReceivedMessage(6), hasMessage(hasPayload(is("f"))));
  }
}
