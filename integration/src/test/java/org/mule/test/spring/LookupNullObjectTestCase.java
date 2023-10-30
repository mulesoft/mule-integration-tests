/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.NULL_OBJECTS_IN_SPRING5_REGISTRY;

import static org.apache.commons.lang3.JavaVersion.JAVA_11;
import static org.apache.commons.lang3.SystemUtils.isJavaVersionAtMost;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeTrue;

import org.mule.test.AbstractIntegrationTestCase;

import javax.transaction.TransactionManager;

import org.junit.BeforeClass;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(NULL_OBJECTS_IN_SPRING5_REGISTRY)
public class LookupNullObjectTestCase extends AbstractIntegrationTestCase {

  // TODO W-14338813
  @BeforeClass
  public static void ignoreJava17() {
    assumeTrue(isJavaVersionAtMost(JAVA_11));
  }

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
