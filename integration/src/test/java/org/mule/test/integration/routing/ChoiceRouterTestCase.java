/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.routing;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static java.lang.Runtime.getRuntime;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.ScopeFeature.ChoiceStory.CHOICE;

import org.mule.functional.api.component.InvocationCountMessageProcessor;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Set;

import org.junit.Ignore;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ROUTERS)
@Story(CHOICE)
public class ChoiceRouterTestCase extends AbstractIntegrationTestCase {

  private static final int LOAD = getRuntime().availableProcessors() * 10;

  private static Set<Thread> capturedThreads;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/routing/choice-router-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    capturedThreads = newKeySet();
  }

  @Override
  protected void doTearDown() throws Exception {
    capturedThreads = null;
  }

  @Test
  public void noDefaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("flow").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("whenRouteCounter"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("afterRouteMpCounter"), is(1));
  }

  @Test
  public void defaultAndNoMatchingRoute() throws Exception {
    Message result = flowRunner("otherwise").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("routeCounter"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("otherwiseCounter"), is(1));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("afterCounter"), is(1));
  }

  @Test
  public void multipleMatchingRoutes() throws Exception {
    Message result = flowRunner("multiple").withPayload(TEST_PAYLOAD).run().getMessage();
    assertThat(result.getPayload().getValue(), is(TEST_PAYLOAD));

    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("first"), is(1));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("second"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("default"), is(0));
    assertThat(InvocationCountMessageProcessor.getNumberOfInvocationsFor("after"), is(1));
  }

  @Test
  public void errorsWithinRouteArePropagated() throws Exception {
    assertMultipleErrors("error-handler", "handled");
  }

  @Test
  public void errorsWithinRouteExpressionArePropagated() throws Exception {
    assertMultipleErrors("expression", "handled");
  }

  @Test
  public void errorsWithinTryRouteArePropagated() throws Exception {
    assertMultipleErrors("try-error-handler", "handled after try");
  }

  @Test
  public void errorsWithinTryRouteExpressionArePropagated() throws Exception {
    assertMultipleErrors("try-expression", "handled after try");
  }

  private void assertMultipleErrors(String expression, String expected) throws Exception {
    for (int i = 0; i < LOAD; i++) {
      Message message = flowRunner(expression).withPayload(TEST_PAYLOAD).run().getMessage();
      assertThat(message, hasPayload(equalTo(expected)));
    }
  }

  @Test
  @Ignore("MULE-18940")
  public void txWithNonBlockingRoute() throws Exception {
    Message result = flowRunner("txNonBlocking").withPayload("nonBlocking").run().getMessage();
    assertThat(capturedThreads, hasSize(1));
  }

  @Test
  @Ignore("MULE-18940")
  public void txWithCpuIntensiveRoute() throws Exception {
    Message result = flowRunner("txCpuIntensive").withPayload("cpuIntensive").run().getMessage();
    assertThat(capturedThreads, hasSize(1));
  }

  @Test
  @Ignore("MULE-18940")
  public void txWithBlockingRoute() throws Exception {
    Message result = flowRunner("txBlocking").withPayload("blocking").run().getMessage();
    assertThat(capturedThreads, hasSize(1));
  }

  @Test
  @Ignore("MULE-18940")
  public void txWithOtherwise() throws Exception {
    Message result = flowRunner("txOtherwise").withPayload("ooo").run().getMessage();
    assertThat(capturedThreads, hasSize(1));
  }

  @Test
  @Ignore("MULE-18940")
  public void txWithNoOtherwise() throws Exception {
    Message result = flowRunner("txNoOtherwise").withPayload("ooo").run().getMessage();
    assertThat(capturedThreads, hasSize(1));
  }

  public static class ThreadCaptor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      capturedThreads.add(currentThread());
      if (capturedThreads.size() > 2) {
        Latch latch = (Latch) event.getVariables().get("latch").getValue();
        if (latch != null) {
          latch.release();
        }
      }

      return event;
    }
  }

}

