/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

@Ignore("See MULE-9195")
@Issue("MULE-9195")
public class MessageRootIdPropagationTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/message-root-id.xml";
  }

  @Test
  public void testRootIDs() throws Exception {
    RootIDGatherer.initialize();

    FlowRunner runner = flowRunner("flow1").withPayload("Hello").withOutboundProperty("where", "client");

    RootIDGatherer.process(runner.buildEvent().getMessage());
    runner.run();
    Thread.sleep(1000);
    assertEquals(6, RootIDGatherer.getMessageCount());
    assertEquals(1, RootIDGatherer.getIds().size());
  }

  public static class RootIDGatherer implements Processor {

    static int messageCount;
    static Map<String, Message> idMap;
    static int counter;


    public static void initialize() {
      idMap = new HashMap<>();
      messageCount = 0;
    }

    public static synchronized void process(Message msg) {
      messageCount++;
      String where = (String) msg.getPayload().getValue();
      if (where == null) {
        where = "location_" + counter++;
      }
      idMap.put(where, msg);
    }

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      process(event.getMessage());
      return event;
    }

    public static Set<Message> getIds() {
      return new HashSet<>(idMap.values());
    }

    public static int getMessageCount() {
      return messageCount;
    }

    public static Map<String, Message> getIdMap() {
      return idMap;
    }
  }
}
