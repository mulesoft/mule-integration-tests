/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.processors;

import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.ParseTemplateStory.PARSE_TEMPLATE;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Stories;
import io.qameta.allure.Story;
import io.qameta.allure.Issue;

@Feature(CORE_COMPONENTS)
@Stories({@Story(PARSE_TEMPLATE), @Story(DSL_VALIDATION_STORY)})
public class ParseTemplateFailuresTestCase extends AbstractConfigurationFailuresTestCase {

  @Test
  @Issue("MULE-19987")
  public void withWrongTargetValue() throws Exception {
    // verify this doesn't fail on initialize
    loadConfiguration("org/mule/processors/parse-template-wrong-target-value-config.xml");
  }

}
