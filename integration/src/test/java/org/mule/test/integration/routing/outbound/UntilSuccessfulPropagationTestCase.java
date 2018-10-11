/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

public class UntilSuccessfulPropagationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/outbound/until-successful-propagation.xml";
  }

  @Test
  public void variablePropagation() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulVariables").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("message executed once"));
  }

  @Test
  public void variableNoPropagation() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulNoPropagationVariables").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), nullValue());
  }

  @Test
  public void variablePropagationNoError() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulVariablesNoError").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("message executed once"));
  }

  @Test
  public void payloadPropagation() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulPayload").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("message"));
  }

  @Test
  public void payloadPropagationNoError() throws Exception {
    CoreEvent event = flowRunner("untilSuccessfulPayloadNoError").withPayload("message").run();
    assertThat(event.getMessage().getPayload().getValue(), is("message executed once"));
  }
}
