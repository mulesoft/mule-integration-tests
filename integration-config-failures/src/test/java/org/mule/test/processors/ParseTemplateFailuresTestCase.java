/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.processors;

import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.ParseTemplateStory.PARSE_TEMPLATE;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import static org.hamcrest.Matchers.containsString;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Stories({@Story(PARSE_TEMPLATE), @Story(DSL_VALIDATION_STORY)})
public class ParseTemplateFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void locationNotExists() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/parse-template-location-not-exists.xml:9]: "
            + "Template location: 'notExists.tem' not found"));
    loadConfiguration("org/mule/processors/parse-template-location-not-exists.xml");
  }

  @Test
  public void locationAndContent() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("[org/mule/processors/parse-template-location-and-content.xml:9]: "
            + "Element <parseTemplate>, the following parameters cannot be set at the same time: [content, location]"));
    loadConfiguration("org/mule/processors/parse-template-location-and-content.xml");
  }

}
