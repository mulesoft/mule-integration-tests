/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Test;

import io.qameta.allure.Feature;

@Feature(EXPRESSION_LANGUAGE)
public class ExpressionLanguageConfigTestCase extends AbstractIntegrationTestCase {

  ExpressionManager el;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-config.xml";
  }

  @Before
  public void setup() {
    el = muleContext.getExpressionManager();
  }

  @Test
  public void testExpressionLanguageExecuteElement() throws Exception {
    flowRunner("flow").withPayload("foo").run();
  }

}
