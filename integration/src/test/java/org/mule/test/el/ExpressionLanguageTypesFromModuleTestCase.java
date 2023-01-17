/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.runtime.api.metadata.DataType.TEXT_STRING;
import static org.mule.runtime.api.metadata.MediaType.APPLICATION_JAVA;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_FUNCTIONS;

import static java.lang.String.format;
import static java.lang.Thread.currentThread;
import static java.lang.Thread.sleep;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.http.HttpStatus.SC_METHOD_NOT_ALLOWED;
import static org.apache.http.HttpStatus.SC_UNAUTHORIZED;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.core.IsIterableContaining.hasItems;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_FUNCTIONS)
public class ExpressionLanguageTypesFromModuleTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public ExpectedError expectedError = ExpectedError.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-types-from-module-config.xml";
  }

  @Test
  public void usingTypeFromModule() throws Exception {
    assertThat(flowRunner("usingTypeFromModule").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("false")));
  }
}
