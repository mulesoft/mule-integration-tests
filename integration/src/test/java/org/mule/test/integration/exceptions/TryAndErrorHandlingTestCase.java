/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;
import static org.mule.functional.api.component.InvocationCountMessageProcessor.getNumberOfInvocationsFor;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;


public class TryAndErrorHandlingTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/try-error-handling.xml";
  }

  @Test
  public void tryInErrorHandler() throws Exception {
    CoreEvent event = flowRunner("tryInErrorHandler").run();
    assertThat(event.getMessage().getPayload().getValue(), is("hello"));
    assertThat(getNumberOfInvocationsFor("try-in-eh"), is(0));
  }

}
