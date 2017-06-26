/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.routing.IdempotentMessageValidator;
import org.mule.runtime.core.routing.IdempotentSecureHashMessageValidator;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.List;

import org.junit.Test;

public class DslConstantsRoutersFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  public String getConfigFile() {
    return "core-namespace-routers-flow.xml";
  }

  @Test
  public void testIdempotentSecureHashReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentSecureHashReceiverRouter");
    assertThat(router, instanceOf(IdempotentSecureHashMessageValidator.class));

    IdempotentSecureHashMessageValidator filter = (IdempotentSecureHashMessageValidator) router;
    assertThat(filter.getMessageDigestAlgorithm(), is("SHA-128"));
    assertThat(filter.getObjectStore(), not(nullValue()));
  }

  @Test
  public void testIdempotentReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentReceiverRouter");
    assertThat(router, instanceOf(IdempotentMessageValidator.class));

    IdempotentMessageValidator filter = (IdempotentMessageValidator) router;
    assertThat(filter.getIdExpression(), is("#[id + '-' + correlationId]"));
    assertThat(filter.getObjectStore(), not(nullValue()));
  }

  @Test
  public void testIdempotentReceiverRouterError() throws Exception {
    assertThat(flowRunner("IdempotentReceiverRouterVar").withVariable("otherId", "123").run()
        .getMessage().getPayload().getValue(),
               is("Not duplicate"));
    assertThat(flowRunner("IdempotentReceiverRouterVar").withVariable("otherId", "123").run()
        .getMessage().getPayload().getValue(),
               is("Duplicate"));
  }

  protected Processor lookupMessageProcessorFromFlow(String flowName) throws Exception {
    Flow flow = lookupFlow(flowName);
    List<Processor> routers = flow.getProcessors();
    assertEquals(1, routers.size());
    return routers.get(0);
  }

  protected Flow lookupFlow(String flowName) {
    Flow flow = muleContext.getRegistry().lookupObject(flowName);
    assertNotNull(flow);
    return flow;
  }
}
