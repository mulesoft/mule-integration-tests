/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.processors;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.ArtifactClassLoaderRunnerConfig;
import org.mule.test.runner.RunnerConfigSystemProperty;

import static org.junit.Assert.assertEquals;
import static org.mule.runtime.api.util.MuleSystemProperties.PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.ParseTemplateStory.PARSE_TEMPLATE;

@Feature(CORE_COMPONENTS)
@Story(PARSE_TEMPLATE)
@ArtifactClassLoaderRunnerConfig(
    systemProperties = {
        @RunnerConfigSystemProperty(
            key = PARSE_TEMPLATE_USE_LEGACY_DEFAULT_TARGET_VALUE,
            value = "true")
    })
public class ParseTemplateLegacyDefaultTargetValueTestCase extends AbstractIntegrationTestCase {

  private static final String PARSED_DW_EXPRESSION =
      "This template has a DW expression to parse from dw-expression flow. Remember, the name of the flow is dw-expression";

  @Override
  public String getConfigFile() {
    return "org/mule/processors/parse-template-config.xml";
  }

  @Test
  @Issue("W-13588449")
  public void withTargetDefaultTargetValueDefinedInline() throws Exception {
    String startingPayload = "Starting payload";
    CoreEvent event = flowRunner("with-target").withPayload(startingPayload).withVariable("flowName", "dw-expression").run();
    Message msg = (Message) event.getVariables().get("targetVar").getValue();
    String previousPayload = (String) event.getMessage().getPayload().getValue();
    assertEquals(PARSED_DW_EXPRESSION, msg.getPayload().getValue());
    assertEquals(previousPayload, startingPayload);
  }

}
