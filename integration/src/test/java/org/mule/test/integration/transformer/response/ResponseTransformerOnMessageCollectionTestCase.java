/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.transformer.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class ResponseTransformerOnMessageCollectionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/transformer/response/response-transformer-on-message-collection-flow-config.xml";
  }

  @Test
  public void transformedDataIsNotLost() throws Exception {
    Message response = flowRunner("Distributor").withPayload(TEST_MESSAGE).run().getMessage();

    assertEquals("foo", response.getPayload().getValue());
    assertFalse(response.getPayload().getValue() instanceof List);
  }
}
