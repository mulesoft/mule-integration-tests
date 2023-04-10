/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isA;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.junit.rules.ExpectedException.none;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class DslConstantsRoutersFlowTestCase extends AbstractIntegrationTestCase {

  @Override
  public String getConfigFile() {
    return "core-namespace-routers-flow.xml";
  }

  @Rule
  public ExpectedException expectedException = none();

  @Test
  public void testIdempotentSecureHashReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentSecureHashReceiverRouter");
    assertThat(router.getClass().getName(),
               equalTo("org.mule.runtime.core.internal.routing.IdempotentMessageValidator"));

    assertThat(getObjectStore(router), not(nullValue()));
  }

  @Test
  public void testIdempotentReceiverRouter() throws Exception {
    Processor router = lookupMessageProcessorFromFlow("IdempotentReceiverRouter");
    assertThat(router.getClass().getName(),
               equalTo("org.mule.runtime.core.internal.routing.IdempotentMessageValidator"));

    assertThat(getIdExpression(router), is("#[id + '-' + correlationId]"));
    assertThat(getObjectStore(router), not(nullValue()));
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

  @Test
  public void testSecureHashIdempotentReceiverRouterError() throws Exception {
    assertThat(flowRunner("IdempotentSecureHashReceiverRouterVar").withVariable("otherId", "123").run()
        .getMessage().getPayload().getValue(),
               is("Not duplicate"));
    assertThat(flowRunner("IdempotentSecureHashReceiverRouterVar").withVariable("otherId", "123").run()
        .getMessage().getPayload().getValue(),
               is("Duplicate"));
  }

  @Test
  public void testNonExistentHashingShouldNotExist() throws Exception {
    expectedException.expectCause(isA(ExpressionRuntimeException.class));
    flowRunner("nonExistentHashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testMD2HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("md2HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testMD5HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("md5HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testSHA1HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("sha1HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testSHA224HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("sha224HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testSHA256HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("sha256HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testSHA384HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("sha384HashFlow").withVariable("otherId", "123").run();
  }

  @Test
  public void testSHA512HashingShouldExist() throws Exception {
    // If no exception, then the test should pass
    flowRunner("sha512HashFlow").withVariable("otherId", "123").run();
  }

  protected Processor lookupMessageProcessorFromFlow(String flowName) throws Exception {
    Flow flow = lookupFlow(flowName);
    List<Processor> routers = flow.getProcessors();
    assertEquals(1, routers.size());
    return routers.get(0);
  }

  protected Flow lookupFlow(String flowName) {
    return registry.<Flow>lookupByName(flowName).get();
  }

  private Object getObjectStore(Processor router)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = router.getClass().getMethod("getObjectStore");
    return method.invoke(router);
  }

  private Object getIdExpression(Processor router)
      throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    Method method = router.getClass().getMethod("getIdExpression");
    return method.invoke(router);
  }
}
