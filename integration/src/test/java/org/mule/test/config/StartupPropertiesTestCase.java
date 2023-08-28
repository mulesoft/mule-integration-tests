/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.Map;

import org.junit.Test;

public class StartupPropertiesTestCase extends AbstractIntegrationTestCase {

  private String STARTUP_PROPERTY_1_KEY = "startupProperty1";
  private String STARTUP_PROPERTY_2_KEY = "startupProperty2";
  private String STARTUP_PROPERTY_1_VALUE = "startupProperty1Value";
  private String STARTUP_PROPERTY_2_VALUE = "startupProperty2Value";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/startup-properties-test.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> registryObjects = new HashMap<>();
    registryObjects.put(STARTUP_PROPERTY_1_KEY, STARTUP_PROPERTY_1_VALUE);
    registryObjects.put(STARTUP_PROPERTY_2_KEY, STARTUP_PROPERTY_2_VALUE);
    return registryObjects;
  }

  @Test
  public void testStartProperties() {
    Object property1 = registry.lookupByName(STARTUP_PROPERTY_1_KEY).get();
    Object property2 = registry.lookupByName(STARTUP_PROPERTY_2_KEY).get();
    assertNotNull(property1);
    assertNotNull(property2);
    assertEquals(STARTUP_PROPERTY_1_VALUE, property1);
    assertEquals(STARTUP_PROPERTY_2_VALUE, property2);
  }
}
