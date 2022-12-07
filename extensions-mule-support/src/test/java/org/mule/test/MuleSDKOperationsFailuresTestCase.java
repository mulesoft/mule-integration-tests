/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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
    expectedException.expectMessage("Using an invalid function within an Mule SDK operation");
    loadConfiguration("mule-operations-using-lookup.xml");
  }

}
