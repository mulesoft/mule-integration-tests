/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.construct.FlowConstruct;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.internal.routing.IdempotentMessageValidator;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

/**
 * Tests for all object stores that can be configured on an {@link IdempotentMessageValidator}.
 */
public class IdempotentMessageValidatorNamespaceHandlerTestCase extends AbstractIntegrationTestCase {

  private static final String KEY = "theKey";
  private static final String VALUE = "theValue";

  private ObjectStore objectStore;

  @Inject
  private ObjectStoreManager osManager;

  @Rule
  public SystemProperty customObjectStore = new SystemProperty("customObjectStore", "customObjectStore");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/idempotent-message-validator-config.xml";
  }

  @Test
  public void testCustomObjectStore() throws Exception {
    objectStore = osManager.getObjectStore(customObjectStore.getValue());
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
    assertTrue(flow instanceof Flow);

    final Flow simpleFlow = (Flow) flow;
    final List<Processor> processors = simpleFlow.getProcessors();
    assertEquals(1, processors.size());

    final Processor firstMP = processors.get(0);
    assertThat(firstMP.getClass().getName(),
               equalTo("org.mule.runtime.core.internal.routing.IdempotentMessageValidator"));

    return firstMP;
  }

  private ObjectStore getObjectStore(Processor router)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = router.getClass().getMethod("getObjectStore");
    return (ObjectStore) method.invoke(router);
  }

  // This is needed to recreate the parsers that have been created previously without the system property set by this test.
  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }
}
