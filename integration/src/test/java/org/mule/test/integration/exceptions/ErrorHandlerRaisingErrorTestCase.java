/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

public class ErrorHandlerRaisingErrorTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/error-handler-raise-error.xml";
  }

  @Test
  public void onErrorPropagateRaisesOtherErrorAndItIsHandled() throws Exception {
    CoreEvent event = flowRunner("onErrorPropagateRaisesOtherError").run();
    assertThat(event.getMessage().getPayload().getValue(), is("And I am Ironman"));
  }

}
