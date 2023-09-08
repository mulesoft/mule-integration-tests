/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.Matchers.containsString;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(DSL_VALIDATION_STORY)
public class ExpressionLanguageConfigurationFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void illegalSyntaxDw() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException.expectMessage(containsString("Missing Expression"));
    loadConfiguration("org/mule/test/el/expression-language-illegal-syntax-dw-config.xml");
  }

  @Override
  protected boolean mockExpressionExecutor() {
    return false;
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(new MuleVersion("4.5.0"));
  }
}
