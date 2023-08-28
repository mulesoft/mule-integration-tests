/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import javax.inject.Inject;

public class PropertyPlaceholderMule2150TestCase extends AbstractIntegrationTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty("systemProperty", "org");

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/property-placeholder-mule-2150-test.xml";
  }

  protected String getProperty(String propertyName) {
    String value = configurationProperties.resolveStringProperty(propertyName).get();
    assertNotNull(propertyName, value);
    return value;
  }

  @Test
  public void testMuleEnvironment() {
    assertThat(getProperty("prop1"), is("value1"));
  }

  @Test
  public void testSpringPropertyPlaceholder() {
    assertThat(getProperty("prop2"), is("value2"));
  }

  @Test
  public void testJavaEnvironment() {
    assertThat(System.getProperty("java.version"), is(getProperty("prop3")));
  }
}
