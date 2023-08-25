/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.tck;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class MuleTestNamespaceFunctionalTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/tck/test-namespace-config-flow.xml";
  }

  @Test
  public void testService3() throws Exception {
    CoreEvent event = flowRunner("testService3").withPayload("foo").run();
    Message message = event.getMessage();
    assertNotNull(message);
    assertThat(event.getError().isPresent(), is(false));
    assertThat(getPayloadAsString(message), is("foo received"));
  }

}
