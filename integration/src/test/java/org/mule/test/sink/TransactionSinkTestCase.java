/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.sink;

import static org.mule.test.allure.AllureConstants.TransactionFeature.TRANSACTION;
import static org.mule.test.allure.AllureConstants.TransactionFeature.XaStory.XA_TRANSACTION;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(TRANSACTION)
@Story(XA_TRANSACTION)
@Issue("W-12128703")
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
