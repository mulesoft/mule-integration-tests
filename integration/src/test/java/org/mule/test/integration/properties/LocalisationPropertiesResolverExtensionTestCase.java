/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static java.util.Arrays.asList;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import javax.inject.Inject;

import org.junit.Test;
import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class LocalisationPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Parameterized.Parameters(name = "{0}")
  public static Collection<String> configs() {
    return asList("properties/localisation-properties-resolver-extension-config.xml");
  }

  @Inject
  private ConfigurationProperties configurationProperties;

  private final String configFile;

  public LocalisationPropertiesResolverExtensionTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void propertiesAreResolvedCorrectly() {
    assertThat(configurationProperties.resolveStringProperty("key1").get(), is("25.837.889,45"));
  }
}

