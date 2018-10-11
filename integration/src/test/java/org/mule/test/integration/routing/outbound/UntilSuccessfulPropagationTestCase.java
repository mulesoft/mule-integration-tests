/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing.outbound;

import static org.hamcrest.CoreMatchers.is;
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
  public void variablePropagationToErrorHandler() throws Exception {
    assertPayloadContent(flowRunner("variablePropagationToErrorHandler").withPayload("message").run());
  }

  @Test
  public void variablePropagationOutside() throws Exception {
    assertPayloadContent(flowRunner("variablePropagationOutside").withPayload("message").run());
  }

  @Test
  public void variablePropagationWithoutError() throws Exception {
    assertPayloadContent(flowRunner("variablePropagationWithoutError").withPayload("message").run());
  }

  @Test
  public void variableImmutableBetweenRetries() throws Exception {
    assertPayloadContent(flowRunner("variableImmutableBetweenRetries").withPayload("message").run());
  }

  @Test
  public void payloadPropagation() throws Exception {
    assertPayloadContent(flowRunner("payloadPropagation").withPayload("message").run());
  }

  @Test
  public void payloadPropagationWithoutError() throws Exception {
    assertPayloadContent(flowRunner("payloadPropagationWithoutError").withPayload("message").run());
  }

  @Test
  public void payloadImmutableBetweenRetries() throws Exception {
    assertPayloadContent(flowRunner("payloadImmutableBetweenRetries").withPayload("message").run());
  }

  public void assertPayloadContent(CoreEvent event) {
    assertThat(event.getMessage().getPayload().getValue(), is("message executed once"));
  }
}
