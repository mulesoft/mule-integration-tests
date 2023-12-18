/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.properties;

import static org.mule.runtime.api.util.MuleSystemProperties.HONOUR_RESERVED_PROPERTIES_PROPERTY;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.tck.junit4.rule.SystemProperty;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

import org.junit.Rule;
import org.junit.Test;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
@Issue("W-14616618")
public class LegacyPropertiesBuilderRootReevaluationTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public SystemProperty legacyPropertiesResolution = new SystemProperty(HONOUR_RESERVED_PROPERTIES_PROPERTY, "false");

  @Rule
  public SystemProperty fileNameSysProp = new SystemProperty("fileName", "hierarchy.yaml");

  @Test
  public void doubleResolutionInLegacyPropertiesResolutionBuilderWorks() throws Exception {
    loadConfiguration("org/mule/test/components/legacy-properties-builder-root-reevaluation-file.xml");
  }

}

