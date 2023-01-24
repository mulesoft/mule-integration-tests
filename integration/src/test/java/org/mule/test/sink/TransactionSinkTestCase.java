/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.sink;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class TransactionSinkTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/sink/transaction-sink-config.xml";
  }

  @Test
  public void testGetTransactionSinks() throws Exception {
    String payload = flowRunner("BeginFlow").run().getMessage().getPayload().getValue().toString();
    assertThat(payload, is("flow begins"));
  }
}
