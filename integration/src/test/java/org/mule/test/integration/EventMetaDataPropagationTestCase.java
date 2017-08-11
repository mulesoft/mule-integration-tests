/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mule.functional.junit4.TestLegacyMessageUtils.getOutboundProperty;
import org.mule.functional.junit4.TestLegacyMessageBuilder;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.meta.AbstractAnnotatedObject;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transformer.AbstractMessageTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.internal.message.InternalMessage;
import org.mule.tck.testmodels.fruit.Apple;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class EventMetaDataPropagationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/event-metadata-propagation-config-flow.xml";
  }

  @Test
  public void testEventMetaDataPropagation() throws Exception {
    flowRunner("component1").withPayload(TEST_PAYLOAD).run();
  }

  public static class DummyComponent extends AbstractAnnotatedObject implements Processor {

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      return InternalEvent.builder(event).message(doProcess(event)).build();
    }

    private Message doProcess(InternalEvent event) throws MuleException {
            if ("component1".equals(getLocation().getRootContainerName())) {
              Map<String, Serializable> props = new HashMap<>();
              props.put("stringParam", "param1");
              props.put("objectParam", new Apple());
              props.put("doubleParam", 12345.6);
              props.put("integerParam", 12345);
              props.put("longParam", (long) 123456789);
              props.put("booleanParam", Boolean.TRUE);

              return new TestLegacyMessageBuilder()
                  .value(Message.builder().value(event.getMessageAsString(muleContext)).build())
                  .outboundProperties(props).build();
            } else {
              InternalMessage msg = (InternalMessage) event.getMessage();
              assertEquals("param1", msg.getInboundProperty("stringParam"));
              final Object o = msg.getInboundProperty("objectParam");
              assertTrue(o instanceof Apple);
              assertEquals(12345.6, 12345.6, msg.<Double>getInboundProperty("doubleParam", 0d));
              assertEquals(12345, msg.<Integer>getInboundProperty("integerParam", 0).intValue());
              assertEquals(123456789, msg.<Long>getInboundProperty("longParam", 0L).longValue());
              assertEquals(Boolean.TRUE, msg.getInboundProperty("booleanParam", Boolean.FALSE));
            }
      return event.getMessage();
    }

  }

  /**
   * Extend AbstractMessageAwareTransformer, even though it's deprecated, to ensure that it keeps working for compatibility with
   * older user-written transformers.
   */
  public static class DummyTransformer extends AbstractMessageTransformer {

    @Override
    public Object transformMessage(InternalEvent event, Charset outputEncoding) throws TransformerException {
      Message msg = event.getMessage();
      assertEquals("param1", getOutboundProperty(msg, "stringParam"));
      final Object o = getOutboundProperty(msg, "objectParam");
      assertTrue(o instanceof Apple);
      assertEquals(12345.6, 12345.6, getOutboundProperty(msg, "doubleParam", 0d));
      assertEquals(12345, getOutboundProperty(msg, "integerParam", 0).intValue());
      assertEquals(123456789, getOutboundProperty(msg, "longParam", 0L).longValue());
      assertEquals(Boolean.TRUE, getOutboundProperty(msg, "booleanParam", Boolean.FALSE));
      return msg;
    }
  }
}
