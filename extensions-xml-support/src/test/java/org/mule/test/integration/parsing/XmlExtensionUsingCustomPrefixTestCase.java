/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.parsing;

import static org.hamcrest.core.StringStartsWith.startsWith;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.util.ComponentLocationProvider;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(XML_SDK)
public class XmlExtensionUsingCustomPrefixTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Inject
  private ConfigurationComponentLocator configurationComponentLocator;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/parsing/xml-module-using-custom-prefix-config.xml";
  }

  @Test
  public void useCustomPrefixInOperation() {
    Component searchIssues = configurationComponentLocator
        .find(Location.builder().globalName("search-issues").addProcessorsPart().addIndexPart(1).build()).get();

    assertThat(ComponentLocationProvider.getSourceCode(searchIssues), startsWith("<httpn"));
  }


}
