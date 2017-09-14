/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mule.functional.api.event.TestLegacyEventUtils.getEffectiveExceptionHandler;
import static org.mule.tck.MuleTestUtils.getExceptionListeners;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.exception.MessagingExceptionHandler;
import org.mule.runtime.core.api.exception.MessagingExceptionHandlerAcceptor;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.processor.FlowAssert;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

import org.junit.Test;

public class ExceptionHandlingTestCase extends AbstractIntegrationTestCase {

  public static final String MESSAGE = "some message";
  private static final String SYTEM_EXCEPTION_HANDLER_CLASSNAME =
      "org.mule.runtime.core.internal.exception.MessagingExceptionHandlerToSystemAdapter";
  private static final String ERROR_HANDLER_CLASSNAME = "org.mule.runtime.core.internal.exception.ErrorHandler";

  private static MessagingExceptionHandler effectiveMessagingExceptionHandler;
  private static CountDownLatch latch;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/exception-handling-test.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    effectiveMessagingExceptionHandler = null;
  }

  @Test
  public void testCustomProcessorInFlow() throws Exception {
    final BaseEvent muleEvent = runFlow("customProcessorInFlow");
    Message response = muleEvent.getMessage();
    assertThat(response, is(notNullValue()));
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
    assertThat(getExceptionListeners(effectiveMessagingExceptionHandler), hasSize(2));
    MessagingExceptionHandlerAcceptor handler = getExceptionListeners(effectiveMessagingExceptionHandler).get(1);
    assertThat(handler.getClass().getName(), equalTo("org.mule.runtime.core.internal.exception.OnErrorPropagateHandler"));
    assertThat(handler.acceptsAll(), is(true));
  }

  @Test
  public void testAsyncInFlow() throws Exception {
    flowRunner("asyncInFlow").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outFlow4", 3000).getRight().get();
    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
  }

  @Test
  public void testUntilSuccessfulInFlow() throws Exception {
    flowRunner("untilSuccessfulInFlow").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outFlow5", 3000).getRight().get();

    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
  }

  @Test
  public void testCustomProcessorInScope() throws Exception {
    LinkedList<String> list = new LinkedList<>();
    list.add(MESSAGE);
    final BaseEvent muleEvent = flowRunner("customProcessorInScope").withPayload(list).run();
    Message response = muleEvent.getMessage();

    assertNotNull(response);
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
  }

  @Test
  public void testCustomProcessorInTransactionalScope() throws Exception {
    flowRunner("customProcessorInTransactionalScope").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outTransactional1", 3000).getRight().get();

    assertNotNull(response);

    FlowAssert.verify("customProcessorInTransactionalScope");

    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
  }

  @Test
  public void testUntilSuccessfulInTransactionalScope() throws Exception {
    testTransactionalScope("untilSuccessfulInTransactionalScope", "test://outTransactional5", emptyMap());
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(ERROR_HANDLER_CLASSNAME));
  }

  @Test
  public void testCustomProcessorInExceptionStrategy() throws Exception {
    flowRunner("customProcessorInExceptionStrategy").withPayload(MESSAGE).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request("test://outStrategy1", 3000).getRight().get();

    assertNotNull(response);

    FlowAssert.verify("customProcessorInExceptionStrategy");

    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(SYTEM_EXCEPTION_HANDLER_CLASSNAME));
  }

  @Test
  public void testAsyncInExceptionStrategy() throws Exception {
    testExceptionStrategy("asyncInExceptionStrategy", emptyMap());
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(SYTEM_EXCEPTION_HANDLER_CLASSNAME));
  }

  @Test
  public void testUntilSuccessfulInExceptionStrategy() throws Exception {
    testExceptionStrategy("untilSuccessfulInExceptionStrategy", emptyMap());
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(SYTEM_EXCEPTION_HANDLER_CLASSNAME));
  }

  @Test
  public void testUntilSuccessfulInExceptionStrategyRollback() throws Exception {
    testExceptionStrategy("untilSuccessfulInExceptionStrategyRollback", emptyMap());
    assertThat(effectiveMessagingExceptionHandler.getClass().getName(), equalTo(SYTEM_EXCEPTION_HANDLER_CLASSNAME));
  }

  @Test
  public void errorThrownByOperationInForeach() throws Exception {
    MessagingException messagingException =
        flowRunner("errorThrownByOperationInForeach").withPayload(asList("1", "2", "3")).runExpectingException();
    assertThat(messagingException.getCause(), instanceOf(ExpressionRuntimeException.class));
  }

  private void testTransactionalScope(String flowName, String expected, Map<String, Serializable> messageProperties)
      throws Exception {
    flowRunner(flowName).withPayload(MESSAGE).withInboundProperties(messageProperties).dispatch();

    MuleClient client = muleContext.getClient();
    Message response = client.request(expected, 3000).getRight().get();

    assertNotNull(response);
  }

  private void testExceptionStrategy(String flowName, Map<String, Serializable> messageProperties) throws Exception {
    latch = spy(new CountDownLatch(2));
    try {
      flowRunner(flowName).withPayload(MESSAGE).withInboundProperties(messageProperties).dispatch();
    } catch (Exception e) {
      // do nothing
    }

    assertFalse(latch.await(3, SECONDS));
    verify(latch).countDown();
  }

  public static class ExecutionCountProcessor implements Processor {

    @Override
    public synchronized BaseEvent process(BaseEvent event) throws MuleException {
      latch.countDown();
      return event;
    }
  }

  public static class ExceptionHandlerVerifierProcessor implements Processor {

    @Override
    public synchronized BaseEvent process(BaseEvent event) throws MuleException {
      effectiveMessagingExceptionHandler = getEffectiveExceptionHandler(event);
      return event;
    }

  }
}
