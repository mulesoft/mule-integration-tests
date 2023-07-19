/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.properties;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Story;

@Story(DSL_VALIDATION_STORY)
public class InvalidSetVariableTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private final String muleConfigPath = "org/mule/properties/invalid-set-variable.xml";

  @Test
  public void emptyVariableNameValidatedBySchema() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(allOf(containsString("variableName"), containsString("set-variable")));
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    loadConfiguration(muleConfigPath);
  }
}
