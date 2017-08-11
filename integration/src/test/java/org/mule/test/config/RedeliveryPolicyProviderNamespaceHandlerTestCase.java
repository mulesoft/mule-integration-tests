/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.internal.routing.IdempotentMessageValidator;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import org.junit.Test;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageValidator}.
 */
public class RedeliveryPolicyProviderNamespaceHandlerTestCase extends AbstractIntegrationTestCase {

  public RedeliveryPolicyProviderNamespaceHandlerTestCase() {
    // we just test the wiring of the objects, no need to start the MuleContext
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/redelivery-policy-config.xml";
  }

  @Test
  public void testInMemoryObjectStore() throws Exception {
    Processor filter = redeliveryPolicyFromFlow("inMemoryStore");

    assertThat(getMaxRedeliveryCount(filter), equalTo(12));
    assertThat(getIdExpression(filter), is(nullValue()));
  }

  @Test
  public void testSimpleTextFileStore() throws Exception {
    Processor filter = redeliveryPolicyFromFlow("simpleTextFileStore");

    assertThat(getMaxRedeliveryCount(filter), equalTo(5));
    assertThat(getIdExpression(filter), equalTo("#[mel:message:id]"));
  }

  @Test
  public void testCustomObjectStore() throws Exception {
    Processor filter = redeliveryPolicyFromFlow("customObjectStore");

    assertThat(getMaxRedeliveryCount(filter), equalTo(5));
    assertThat(getIdExpression(filter), is(nullValue()));
  }

  private Processor redeliveryPolicyFromFlow(String flowName) throws Exception {
    Flow flow = (Flow) getFlowConstruct(flowName);
    Processor messageProcessor = flow.getProcessors().get(0);
    assertThat(messageProcessor.getClass().getName(),
               equalTo("org.mule.runtime.core.privileged.processor.IdempotentRedeliveryPolicy"));
    return messageProcessor;
  }

  private Object getMaxRedeliveryCount(Processor processor)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = processor.getClass().getMethod("getMaxRedeliveryCount");
    return method.invoke(processor);
  }


  private Object getIdExpression(Processor processor)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = processor.getClass().getMethod("getIdExpression");
    return method.invoke(processor);
  }

  public static class CustomObjectStore extends SimpleMemoryObjectStore<Serializable> {

    private String customProperty;

    public String getCustomProperty() {
      return customProperty;
    }

    public void setCustomProperty(String value) {
      customProperty = value;
    }
  }
}
