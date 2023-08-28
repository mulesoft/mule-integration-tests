/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.pojos.ParameterCollectionParser;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

public class PropertiesTestCase extends AbstractIntegrationTestCase {

  public static final String SYSTEM_PROPERTY_VALUE = "systemPropertyValue";

  @Rule
  public SystemProperty lastnameSystemProperty = new SystemProperty("systemProperty", SYSTEM_PROPERTY_VALUE);
  @Rule
  public SystemProperty ageSystemProperty = new SystemProperty("testPropertyOverrided", "10");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/dsl/properties-config.xml";
  }

  @Test
  public void propertiesAreCorrectlyConfigured() {
    ParameterCollectionParser parsersTestObject = registry.<ParameterCollectionParser>lookupByName("testObject").get();
    assertThat(parsersTestObject.getFirstname(), is("testPropertyValue"));
    assertThat(parsersTestObject.getLastname(), is(SYSTEM_PROPERTY_VALUE));
    assertThat(parsersTestObject.getAge(), is(10));
  }

  @Test
  @Issue("MULE-19271")
  public void configurationPropertiesAreCorrectlyConfigured() {
    assertThat(muleContext.getConfiguration().getDefaultResponseTimeout(), is(1000));
  }

}
