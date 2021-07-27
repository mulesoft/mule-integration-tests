/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.tck;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCause;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.FileNotFoundException;

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

  @Test
  public void testService4() throws Exception {
    flowRunner("testService4").withPayload("foo").runExpectingException();
  }

  @Test
  public void testService5() throws Exception {
    Exception e = flowRunner("testService5").withPayload("foo").runExpectingException();
    assertTrue(getRootCause(e) instanceof FileNotFoundException);
  }
}
