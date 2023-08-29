/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@Issue("MULE-20057")
@RunnerDelegateTo(Parameterized.class)
public class InFlowCustomPropertiesResolverExtensionTestCase extends AbstractIntegrationTestCase {

  @Parameters(name = "{0}")
  public static Collection<String> configs() {
    return asList("properties/custom-properties-resolver-extension-config.xml",
                  "properties/custom-properties-resolver-extension-deprecated-config.xml");
  }

  private final String configFile;

  public InFlowCustomPropertiesResolverExtensionTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        configFile,
        "properties/custom-properties-resolver-extension-flows-config.xml"
    };
  }

  @Test
  public void usePlaceholder() throws Exception {
    assertThat(flowRunner("usePlaceholder").run().getMessage().getPayload().getValue(),
               is("test.key1:value1:AES:CBC"));
  }

  @Test
  public void usePFunction() throws Exception {
    assertThat(flowRunner("usePFunction").run().getMessage().getPayload().getValue(),
               is("test.key1:value1:AES:CBC"));
  }

}
