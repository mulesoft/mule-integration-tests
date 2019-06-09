/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static java.lang.Runtime.getRuntime;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ChoiceStory.CHOICE;
import org.mule.functional.api.component.InvocationCountMessageProcessor;
import org.mule.runtime.api.message.Message;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(ROUTERS)
@Story(CHOICE)
public class ChoiceRouterTestCase extends AbstractIntegrationTestCase {

  private static final int LOAD = getRuntime().availableProcessors() * 10;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/choice-router-config.xml";
  }

  @Test
  public void noDefaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("flow").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("whenRouteCounter"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("afterRouteMpCounter"), is(1));
  }

  @Test
  public void defaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("otherwise").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("routeCounter"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("otherwiseCounter"), is(1));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("afterCounter"), is(1));
  }

  @Test
  public void multipleMatchingRoutes() throws Exception {
    Message result = flowRunner("multiple").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("first"), is(1));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("second"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("default"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("after"), is(1));
  }

  @Test
  public void errorsWithinRouteArePropagated() throws Exception {
    assertMultipleErrors("error-handler");
  }

  @Test
  public void errorsWithinRouteExpressionArePropagated() throws Exception {
    assertMultipleErrors("expression");
  }

  private void assertMultipleErrors(String expression) throws Exception {
    for (int i = 0; i < LOAD; i++) {
      Message message = flowRunner(expression).withPayload(TEST_PAYLOAD).run().getMessage();
      assertThat(message, hasPayload(equalTo("handled")));
    }
  }

}

