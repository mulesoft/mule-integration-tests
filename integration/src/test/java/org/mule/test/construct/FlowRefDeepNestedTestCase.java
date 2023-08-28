/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.FlowReferenceStory.FLOW_REFERENCE;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(CORE_COMPONENTS)
@Story(FLOW_REFERENCE)
public class FlowRefDeepNestedTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/construct/flow-ref-deep-nested.xml";
  }

  @Inject
  @Named("rootJustSubFlows")
  private Flow rootJustSubFlows;

  @Test
  @Issue("MULE-18694")
  @Description("Verify that apps with deep subflow nesting work as expected, falling back to Mono")
  public void deeplyNestedSubFlows() throws Exception {
    rootJustSubFlows.start();
    try {
      flowRunner("rootJustSubFlows").run();
    } finally {
      rootJustSubFlows.stop();
    }
  }
}
