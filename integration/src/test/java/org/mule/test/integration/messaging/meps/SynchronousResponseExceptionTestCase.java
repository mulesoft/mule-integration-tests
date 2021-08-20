/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.messaging.meps;

import static org.mule.runtime.core.api.error.Errors.CORE_NAMESPACE_NAME;
import static org.mule.runtime.core.api.error.Errors.Identifiers.ROUTING_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

/**
 * see MULE-4512
 */
public class SynchronousResponseExceptionTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/messaging/meps/synchronous-response-exception-flow.xml";
  }

  @Test
  public void testComponentException() throws Exception {
    flowRunner("ComponentException").withPayload("request").runExpectingException(errorType("TEST", "EXPECTED"));
  }

  @Test
  public void testFlowRefInvalidException() throws Exception {
    flowRunner("FlowRefInvalidException").withPayload("request")
        .runExpectingException(errorType(CORE_NAMESPACE_NAME, ROUTING_ERROR_IDENTIFIER));
  }

}
