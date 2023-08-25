/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(MULE_DSL)
@Story(DSL_VALIDATION_STORY)
public class MuleSDKOperationsFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  @Ignore("W-12074712")
  @Description("An operation cannot use lookup function (even without explicit binding)")
  public void returningTypeFromDependency() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage("Using an invalid function within a Mule SDK operation");
    loadConfiguration("mule-operations-using-lookup.xml");
  }

}
