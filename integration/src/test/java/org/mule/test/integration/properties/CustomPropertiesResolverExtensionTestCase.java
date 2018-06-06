/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.config.api.dsl.model.properties.ConfigurationPropertiesProvider;
import org.mule.runtime.config.internal.dsl.model.config.ConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.DefaultConfigurationPropertiesResolver;
import org.mule.runtime.config.internal.dsl.model.config.PropertiesResolverConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.crafted.config.properties.extension.SecureConfigurationPropertiesProvider;

import java.lang.reflect.Field;
import java.util.Optional;

import javax.inject.Inject;

import org.junit.Test;

public class CustomPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ConfigurationProperties configurationProperties;

  @Override
  protected void doTearDownAfterMuleContextDispose() throws Exception {
    //Also check that provider is disposed
    assertThat(configurationProperties.resolveStringProperty("lifecycle::initialize").get(), is("1"));
    assertThat(configurationProperties.resolveStringProperty("lifecycle::dispose").get(), is("1"));
  }

  @Override
  protected String getConfigFile() {
    return "properties/custom-properties-resolver-extension-config.xml";
  }

  @Test
  public void propertiesAreResolvedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is("test.key1:value1:AES:CBC"));
    assertThat(configurationProperties.resolveStringProperty("key2").get(), is("test.key2:value2:AES:CBC"));
  }

  @Test
  public void providerIsInitialisedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("lifecycle::initialize").get(), is("1"));
    assertThat(configurationProperties.resolveStringProperty("lifecycle::dispose").get(), is("0"));
  }

}
