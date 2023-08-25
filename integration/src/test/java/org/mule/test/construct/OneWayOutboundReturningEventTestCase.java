/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.api.message.Message;

public class OneWayOutboundReturningEventTestCase extends OneWayOutboundTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/one-way-outbound-config.xml";
  }

  @Override
  protected void assertOneWayOutboundResponse(Message response) {
    assertEquals("TEST", response.getPayload().getValue());
  }

  @Override
  protected void assertOneWayOutboundAfterComponentResponse(Message response) {
    assertEquals("TEST processed", response.getPayload().getValue());
  }
}
