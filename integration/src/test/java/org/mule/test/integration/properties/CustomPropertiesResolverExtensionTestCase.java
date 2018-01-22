/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class CustomPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  protected String getConfigFile() {
    return "properties/custom-properties-resolver-extension-config.xml";
  }

  @Test
  public void propertiesAreResolvedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is("test.key1:value1:AES:CBC"));
    assertThat(configurationProperties.resolveStringProperty("key2").get(), is("test.key2:value2:AES:CBC"));
  }
}
