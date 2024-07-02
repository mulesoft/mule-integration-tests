/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Issue;
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

  @Test
  @Issue("W-15509819")
  public void failureOnImportWithDifferentArtifactType() throws Exception {
    String configFile = "org/mule/config/spring/parsers/dsl-validation-different-artifact-type-import-config.xml";
    expectedException.expectMessage("[" + configFile + ":7]: "
        + "Imported resource 'org/mule/config/spring/parsers/dsl-validation-different-artifact-type-imported-config.xml' declares a 'DOMAIN', not a 'APPLICATION'");
    loadConfiguration(configFile);
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(new MuleVersion("4.8.0"));
  }

}
