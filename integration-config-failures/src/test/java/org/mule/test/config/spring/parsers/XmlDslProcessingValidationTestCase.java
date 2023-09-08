/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static java.util.Collections.emptySet;
import static org.junit.rules.ExpectedException.none;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.tests.api.TestComponentsExtension;

import java.util.ArrayList;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Story;

// TODO MULE-18446 Migrate this test to a unit test where the implementation of the validation will be
@Story(DSL_VALIDATION_STORY)
public class XmlDslProcessingValidationTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void parameterAndChildAtOnce() throws Exception {
    String configFile = "org/mule/config/spring/parsers/dsl-validation-duplicate-pojo-or-list-parameter-config.xml";
    expectedException.expectMessage("[" + configFile + ":9]: "
        + "Component 'test-components:element-with-attribute-and-child' has a child element 'test-components:my-pojo'"
        + " which is used for the same purpose of the configuration parameter 'myPojo'. Only one must be used.");
    loadConfiguration(configFile);
  }

  @Test
  public void emptyChildSimpleParameter() throws Exception {
    String configFile = "org/mule/config/spring/parsers/dsl-validation-empty-simple-child-parameter.xml";

    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage("[" + configFile + ":9]: Element <test-components:text-pojo> is missing required parameter 'text'.");
    loadConfiguration(configFile);
  }

  @Override
  protected List<ExtensionModel> getRequiredExtensions() {
    ExtensionModel testComponents = loadExtension(TestComponentsExtension.class, emptySet());

    final List<ExtensionModel> extensions = new ArrayList<>();
    extensions.add(testComponents);

    return extensions;
  }
}
