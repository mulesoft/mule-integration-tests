/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.UntilSuccessfulStory.UNTIL_SUCCESSFUL;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

@Feature(ROUTERS)
@Story(UNTIL_SUCCESSFUL)
@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulPropagationTestCase extends AbstractIntegrationTestCase {

  private final String flow;

  public UntilSuccessfulPropagationTestCase(String flow) {
    this.flow = flow;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/routing/until-successful-propagation.xml";
  }

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object> data() {
    return asList(
                  "variablePropagationToErrorHandler",
                  "variablePropagationOutside",
                  "variablePropagationWithoutError",
                  "variableImmutableBetweenRetries",
                  "payloadPropagation",
                  "payloadPropagationWithoutError",
                  "payloadImmutableBetweenRetries");
  }

  @Test
  public void runTest() throws Exception {
    assertPayloadContent(flowRunner(flow).withPayload("message").run());
  }

  public void assertPayloadContent(CoreEvent event) {
    assertThat(event.getMessage().getPayload().getValue(), is("message executed once"));
  }
}
