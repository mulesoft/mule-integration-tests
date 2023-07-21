/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.spring.parsers;

import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Story;

@Story(DSL_VALIDATION_STORY)
public class ImportValidationTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void failureOnInvalidImport() throws Exception {
    String configFile = "org/mule/config/spring/parsers/dsl-validation-invalid-import-config.xml";
    expectedException.expectMessage("[" + configFile + ":7]: "
        + "Could not find imported resource 'invalid_location.xml'");
    loadConfiguration(configFile);
  }

}
