/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.integration.exceptions.OnErrorContinueTestCase.JSON_REQUEST;
import static org.mule.test.integration.exceptions.OnErrorContinueTestCase.JSON_RESPONSE;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.transaction.Transaction;
import org.mule.runtime.core.api.transaction.TransactionCoordination;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Arrays;
import java.util.Collection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class OnErrorContinueFlowRefTestCase extends AbstractIntegrationTestCase {

  public static final int TIMEOUT = 5000;
  private final String config;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {
        {"Local Error Handler", "org/mule/test/integration/exceptions/on-error-continue-flow-ref.xml"},
        {"Global Error Handler", "org/mule/test/integration/exceptions/on-error-continue-flow-ref-global-err.xml"}
    });
  }


  public OnErrorContinueFlowRefTestCase(String type, String config) {
    this.config = config;
  }

  @Override
  protected String getConfigFile() {
    return config;
  }

  @Test
  public void testFlowRefHandlingException() throws Exception {
    Message response = flowRunner("exceptionHandlingBlock").withPayload(JSON_REQUEST).run().getMessage();
    // compare the structure and values but not the attributes' order
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  @Test
  public void testFlowRefHandlingExceptionWithTransaction() throws Exception {
    Message response = flowRunner("transactionNotResolvedAfterException").withPayload(JSON_REQUEST).run().getMessage();
    // compare the structure and values but not the attributes' order
    ObjectMapper mapper = new ObjectMapper();
    JsonNode actualJsonNode = mapper.readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = mapper.readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  public static class VerifyTransactionNotResolvedProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      Transaction tx = TransactionCoordination.getInstance().getTransaction();
      assertThat(tx, notNullValue());
      assertThat(tx.isRollbackOnly(), is(false));
      return event;
    }
  }
}
