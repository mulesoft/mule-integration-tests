/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.containsString;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.config.ConfigurationException;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Issue("MULE-18770")
@Feature(CORRELATION_ID)
@Story(DSL_VALIDATION_STORY)
public class SourceCorrelationIdErrorGenerationTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void errorStaticValue() throws Exception {
    expectedException.expect(ConfigurationException.class);
    expectedException
        .expectMessage(containsString("A static value (''doge'') was given for parameter 'correlationIdGeneratorExpression' but it requires an expression"));
    loadConfiguration("org/mule/test/config/correlation-id/static-generation.xml");
  }

  @Test
  public void invalidExpression() throws Exception {
    expectedException.expect(InitialisationException.class);
    expectedException.expectMessage(containsString("Invalid Correlation ID Generation expression: #['some]"));
    loadConfiguration("org/mule/test/config/correlation-id/error-generation.xml");
  }

}
