/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

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
  public void errorsWithinRouteArePropagated() throws Exception {
    Message message = flowRunner("error-handler").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(message, hasPayload(equalTo("handled")));
  }

}

