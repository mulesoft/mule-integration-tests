/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.transformer.response;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.test.AbstractIntegrationTestCase;

import java.nio.charset.Charset;
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

  public static class ReturnFooTransformer extends AbstractTransformer {

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      return "foo";
    }
  }
}
