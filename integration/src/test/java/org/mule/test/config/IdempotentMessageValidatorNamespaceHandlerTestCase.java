/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.IdempotentMessageValidator;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageValidator}.
 */
public class IdempotentMessageValidatorNamespaceHandlerTestCase extends AbstractIntegrationTestCase {

  private static final String KEY = "theKey";
  private static final String VALUE = "theValue";

  private ObjectStore objectStore;

  @Rule
  public SystemProperty customObjectStore = new SystemProperty("customObjectStore", "customObjectStore");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/idempotent-message-validator-config.xml";
  }

  @Test
  public void testCustomObjectStore() throws Exception {
    objectStore = muleContext.getObjectStoreManager().getObjectStore(customObjectStore.getValue());
    objectStore.store(KEY, VALUE);
    testPojoObjectStore("customObjectStoreFlow");
  }

  private void testPojoObjectStore(final String flowName) throws Exception {
    final Processor filter = idempotentMessageFilterFromFlow(flowName);

    final ObjectStore<?> store = getObjectStore(filter);
    assertThat(store.contains(KEY), equalTo(true));
  }

  private Processor idempotentMessageFilterFromFlow(final String flowName) throws Exception {
    final FlowConstruct flow = registry.<FlowConstruct>lookupByName(flowName).get();
    assertThat(flow, instanceOf(Flow.class));

    final Flow simpleFlow = (Flow) flow;
    final List<Processor> processors = simpleFlow.getProcessors();
    assertThat(processors, hasSize(1));

    final Processor firstMP = processors.get(0);
    assertThat(firstMP.getClass().getName(), equalTo("org.mule.runtime.core.internal.routing.IdempotentMessageValidator"));

    return firstMP;
  }

  private ObjectStore getObjectStore(Processor router)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = router.getClass().getMethod("getObjectStore");
    return (ObjectStore) method.invoke(router);
  }

  @Override
  protected boolean mustRegenerateAstXmlParser() {
    return true;
  }
}
