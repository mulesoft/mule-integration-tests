/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_FUNCTIONS;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_FUNCTIONS)
public class ExpressionLanguageTypesFromModuleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-types-from-module-config.xml";
  }

  @Test
  public void usingTypeFromModule() throws Exception {
    assertThat(flowRunner("usingTypeFromModule").run().getMessage(), hasPayload(equalTo("false")));
  }
}
