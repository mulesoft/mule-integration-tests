/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.munit.component.location;

import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Issue;

public class MUnitEnableFlowSourcesTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/munit/component/flows/munit-enable-flow-sources-subflow.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Test
  @Issue("MULE-19110")
  public void enableFlowSourcesSubflow() throws Exception {
    Location location = builder().globalName("munit-enable-flow-sources-subflow").build();
    lazyComponentInitializer.initializeComponent(location);
  }

}
