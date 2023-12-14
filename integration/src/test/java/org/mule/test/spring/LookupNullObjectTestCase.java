/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.LIFECYCLE_AND_DEPENDENCY_INJECTION;
import static org.mule.test.allure.AllureConstants.LifecycleAndDependencyInjectionFeature.NULL_OBJECTS_IN_SPRING5_REGISTRY;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.test.AbstractIntegrationTestCase;


import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(LIFECYCLE_AND_DEPENDENCY_INJECTION)
@Story(NULL_OBJECTS_IN_SPRING5_REGISTRY)
public class LookupNullObjectTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {};
  }

  @Test
  public void noTransactionManagerThroughRegistryLookup() {
    assertThat(registry.lookupByType(IAmSearchingForYou.class).isPresent(), is(false));
    assertThat(registry.lookupAllByType(IAmSearchingForYou.class), hasSize(0));
  }

  public interface IAmSearchingForYou {

  }
}
