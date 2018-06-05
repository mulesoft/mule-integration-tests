/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.config.api.dsl.model.ConfigurationParameters;
import org.mule.runtime.config.api.dsl.model.ResourceProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProviderFactory;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;


public class CustomConfigurationPropertiesLifecycleTestCase extends AbstractIntegrationTestCase {


  @Override
  protected String getConfigFile() {
    return "custom-object-serializer-config.xml";
  }

  @Test
  public void test() {
    int a = 5;
  }

  public static class CustomConfigurationPropertiesProviderFactory implements ConfigurationPropertiesProviderFactory {

    @Override
    public ComponentIdentifier getSupportedComponentIdentifier() {
      return null;
    }

    @Override
    public ConfigurationPropertiesProvider createProvider(ConfigurationParameters parameters, ResourceProvider externalResourceProvider) {
      return null;
    }
  }

}
