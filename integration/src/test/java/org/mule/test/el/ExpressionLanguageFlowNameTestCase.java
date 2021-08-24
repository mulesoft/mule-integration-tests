/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_EXPRESSION_BINDINGS;

import io.qameta.allure.Story;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Issue;
import io.qameta.allure.Feature;
import org.junit.Test;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_EXPRESSION_BINDINGS)
public class ExpressionLanguageFlowNameTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-flow-name.xml";
  }

  @Test
  @Issue("MULE-19732")
  public void resolveFlowName() throws Exception {
    assertThat(flowRunner("flow-name").keepStreamsOpen().run().getMessage(),
               hasPayload(equalTo("flow-name echoed by Heisenberg")));
  }
}
