/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.processors;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.ParseTemplateStory.PARSE_TEMPLATE;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import io.qameta.allure.Issue;
import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.config.MuleRuntimeFeature;
import org.mule.runtime.api.meta.MuleVersion;
import org.mule.runtime.core.api.config.ConfigurationException;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import org.mule.runtime.core.api.config.DefaultMuleConfiguration;

@Feature(CORE_COMPONENTS)
@Stories({@Story(PARSE_TEMPLATE), @Story(DSL_VALIDATION_STORY)})
@RunWith(Parameterized.class)
public class RequiredExpressionsFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  /**
   * Configures the switch for {@link MuleRuntimeFeature#ENFORCE_REQUIRED_EXPRESSION_VALIDATION}.
   */
  @Parameterized.Parameters(name = "version: {0}")
  public static Collection<Object[]> featureFlags() {
    ExpectedException expected = none();
    expected.expect(ConfigurationException.class);
    expected
        .expectMessage(containsString("A static value ('not_an_expression') was given for parameter 'targetValue' but it requires an expression"));

    return asList(new Object[][] {
        {"4.5.0", expected},
        {"4.4.0", none()}
    });
  }

  public MuleVersion minMuleVersion;

  @Rule
  public ExpectedException expectedException;

  public RequiredExpressionsFailuresTestCase(String minMuleVersion, ExpectedException expectedException) {
    this.minMuleVersion = new MuleVersion(minMuleVersion);
    this.expectedException = expectedException;
  }

  @Test
  @Issue("MULE-19987")
  public void withWrongTargetValue() throws Exception {
    loadConfiguration("org/mule/processors/parse-template-wrong-target-value-config.xml");
  }

  @Override
  protected void applyConfiguration(DefaultMuleConfiguration muleConfiguration) {
    super.applyConfiguration(muleConfiguration);

    muleConfiguration.setMinMuleVersion(minMuleVersion);
  }
}
