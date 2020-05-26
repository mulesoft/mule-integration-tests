/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

public class GlobalErrorHandlerTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/global-error-handler.xml";
  }

  @Test
  public void errorHandlerWithSelfReference() throws Exception {
    // This should fail, but in 4.2 it didn't so we must accept it in 4.3, since it does work
    CoreEvent event = flowRunner("flowWithErrorHandlerSelfReferencing").run();
    assertThat(event.getMessage().getPayload().getValue(), is("Chocotorta"));
  }
}
