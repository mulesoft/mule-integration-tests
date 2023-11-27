/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.el;

import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.EXPRESSION_LANGUAGE;
import static org.mule.test.allure.AllureConstants.ExpressionLanguageFeature.ExpressionLanguageStory.SUPPORT_EXPRESSION_BINDINGS;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(EXPRESSION_LANGUAGE)
@Story(SUPPORT_EXPRESSION_BINDINGS)
public class ExpressionLanguageDataTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/el/expression-language-data-type.xml";
  }

  @Test
  @Issue("TD-0171856")
  public void resolveFlowName() throws Exception {
    final CoreEvent result = flowRunner("flow-name")
        .withPayload(TEST_PAYLOAD)
        .withMediaType(TEXT)
        .run();

    assertThat(result.getMessage().getPayload().getValue().toString(),
               equalTo(TEXT.toRfcString()));
  }
}
