/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.el;

import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;

import static org.junit.Assert.assertEquals;

import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.test.AbstractIntegrationTestCase;

import java.text.DateFormat;
import java.util.Locale;

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
  public void testExpressionLanguageImport() {
    assertEquals(Locale.class, evaluate("loc"));
  }

  @Test
  public void testExpressionLanguageImportNoName() {
    assertEquals(DateFormat.class, evaluate("DateFormat"));
  }

  @Test
  public void testExpressionLanguageAlias() {
    assertEquals(muleContext.getConfiguration().getId(), evaluate("appName"));
  }

  @Test
  public void testExpressionLanguageExecuteElement() throws Exception {
    flowRunner("flow").withPayload("foo").run();
  }

  private Object evaluate(String expression) {
    return el.evaluate("mel:" + expression).getValue();
  }

}
