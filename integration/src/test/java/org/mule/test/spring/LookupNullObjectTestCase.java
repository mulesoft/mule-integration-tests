/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.spring;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.NULL_OBJECTS_IN_SPRING5_REGISTRY;
import org.mule.test.AbstractIntegrationTestCase;

import javax.transaction.TransactionManager;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(NULL_OBJECTS_IN_SPRING5_REGISTRY)
public class LookupNullObjectTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Test
  public void noTransactionManagerThroughMuleContext() {
    TransactionManager txManager = muleContext.getTransactionManager();
    assertThat(txManager, is(nullValue()));
  }

  @Test
  public void noTransactionManagerThroughRegistryLookup() {
    assertThat(registry.lookupByType(TransactionManager.class).isPresent(), is(false));
    assertThat(registry.lookupAllByType(TransactionManager.class), hasSize(0));
  }
}
