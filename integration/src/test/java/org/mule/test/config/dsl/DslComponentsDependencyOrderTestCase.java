/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.dsl;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.MuleDsl.DslParsingStory.DSL_PARSING_STORY;
import static org.mule.test.allure.AllureConstants.MuleDsl.MULE_DSL;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(MULE_DSL)
@Story(DSL_PARSING_STORY)
public class DslComponentsDependencyOrderTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort httpPort1 = new DynamicPort("httpPort1");
  @Rule
  public DynamicPort httpPort2 = new DynamicPort("httpPort2");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/component-dependency-config.xml";
  }

  @Description("Verifies that lifecycle is applied correctly despite the order of the components in the configuration")
  @Test
  public void configurationComponentDependencyOrder() throws Exception {
    String clientFlowName = "clientFlow";
    String portVarName = "port";
    String value = (String) flowRunner(clientFlowName).withVariable(portVarName, httpPort1.getValue()).run()
        .getMessage().getPayload().getValue();
    assertThat(value, is("listener1"));
    value = (String) flowRunner(clientFlowName).withVariable(portVarName, httpPort2.getValue()).run().getMessage()
        .getPayload().getValue();
    assertThat(value, is("listener2"));
  }
}
