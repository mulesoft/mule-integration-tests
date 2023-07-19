/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static java.lang.String.format;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(DSL_VALIDATION_STORY)
@RunWith(Parameterized.class)
public class ErrorHandlingConfigurationFailuresWaivesTestCase extends AbstractConfigurationFailuresTestCase {

  /**
   * Configures the switch for {@link MuleRuntimeFeature#ENFORCE_ERROR_TYPES_VALIDATION}.
   */
  @Parameters(name = "version: {0}")
  public static Collection<Object[]> featureFlags() {
    ExpectedException expected = none();
    expected.expect(ConfigurationException.class);
    expected.expectMessage(containsString(notFound("APP:NONEXISTENT")));

    return asList(new Object[][] {
        {"4.5.0", expected},
        {"4.4.0", none()}
    });
  }

  public MuleVersion minMuleVersion;

  @Rule
  public ExpectedException expectedException;

  public ErrorHandlingConfigurationFailuresWaivesTestCase(String minMuleVersion, ExpectedException expectedException) {
    this.minMuleVersion = new MuleVersion(minMuleVersion);
    this.expectedException = expectedException;
  }

  @Test
  @Issue("MULE-19879")
  public void unknownErrorFilteringNotAllowed() throws Exception {
    loadConfiguration("org/mule/test/integration/exceptions/global-unreferenced-invalid-error-handler.xml");
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(minMuleVersion);
  }

  private static String notFound(String type) {
    return format("Could not find error '%s'", type);
  }

}
