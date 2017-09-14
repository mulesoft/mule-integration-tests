/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.routing;

import static java.lang.Thread.currentThread;
import static java.util.concurrent.ConcurrentHashMap.newKeySet;
import static org.apache.commons.io.IOUtils.LINE_SEPARATOR;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.startsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.functional.junit4.matchers.ThrowableMessageMatcher.hasMessage;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.TEXT;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ROUTERS;
import static org.mule.test.allure.AllureConstants.RoutersFeature.ScatterGatherStory.SCATTER_GATHER;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.message.Error;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.event.BaseEvent;
import org.mule.runtime.core.api.exception.MessagingException;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.routing.CompositeRoutingException;
import org.mule.runtime.core.api.util.concurrent.Latch;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.ByteArrayInputStream;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(ROUTERS)
@Story(SCATTER_GATHER)
public class ScatterGatherRouterTestCase extends AbstractIntegrationTestCase {

  private static final String EXCEPTION_MESSAGE_TITLE_PREFIX = "Exception(s) were found for route(s): " + LINE_SEPARATOR;
  private static Set<Thread> capturedThreads;

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String getConfigFile() {
    return "scatter-gather-test.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    capturedThreads = newKeySet();
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy.")
  public void minimalConfiguration() throws Exception {
    flowRunner("minimalConfig").run();
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy and target configured.")
  public void minimalConfigurationTarget() throws Exception {
    flowRunner("minimalConfigTarget").run();
  }

  @Test
  @Description("Minimal configuration with default collect-map strategy and target configured with targetType Message.")
  public void minimalConfigurationTargetMessage() throws Exception {
    flowRunner("minimalConfigTargetMessage").run();
  }

  @Test
  @Description("Minimal configuration with default with collect-list strategy configured.")
  public void minimalConfigurationCollectList() throws Exception {
    flowRunner("minimalConfigCollectList").run();
  }

  @Test
  @Description("Router request fails with runtime exception is payload is consumable.")
  public void consumablePayload() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(MuleRuntimeException.class));
    expectedException.expectCause(hasMessage(startsWith("Cannot copy message with a stream payload")));
    flowRunner("minimalConfig").withPayload(new ByteArrayInputStream("hello world".getBytes())).run();
  }

  @Test
  @Description("Router times out if routes take longer than the timeout configured to complete.")
  public void timeout() throws Exception {
    expectedException.expect(MessagingException.class);
    expectedException.expectCause(instanceOf(CompositeRoutingException.class));
    flowRunner("timeout").run();
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithException() throws Exception {
    assertRouteException("routeWithException", EXCEPTION_MESSAGE_TITLE_PREFIX
        + "\t1: org.mule.functional.api.exception.FunctionalTestException: Functional Test Service Exception",
                         FunctionalTestException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionWithMessage() throws Exception {
    assertRouteException("routeWithExceptionWithMessage",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\t1: org.mule.functional.api.exception.FunctionalTestException: I'm a message",
                         FunctionalTestException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithNonMuleException() throws Exception {
    assertRouteException("routeWithNonMuleException",
                         EXCEPTION_MESSAGE_TITLE_PREFIX + "\t1: java.lang.NullPointerException: nonMule",
                         NullPointerException.class);
  }

  @Test
  @Description("An error in a route results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExpressionException() throws Exception {
    assertRouteException("routeWithExpressionException", EXCEPTION_MESSAGE_TITLE_PREFIX
        + "\t1: org.mule.runtime.core.api.expression.ExpressionRuntimeException: \"Script 'invalidExpr ' has errors: \n"
        + "\tUnable to resolve reference of invalidExpr. at 1 : 1\" evaluating expression: \"invalidExpr\".",
                         ExpressionRuntimeException.class);
  }

  @Test
  @Description("An error in a route when executing sequentially results in a CompositeRoutingException containing details of exceptions.")
  public void routeWithExceptionInSequentialProcessing() throws Exception {
    assertRouteException("routeWithExceptionInSequentialProcessing",
                         EXCEPTION_MESSAGE_TITLE_PREFIX
                             + "\t1: org.mule.functional.api.exception.FunctionalTestException: Functional Test Service Exception",
                         FunctionalTestException.class);
  }

  private void assertRouteException(String flow, String exceptionMessageStart, Class exceptionType) throws Exception {
    try {
      flowRunner(flow).run();
      fail("Was expecting a failure");
    } catch (MessagingException e) {
      assertThat(e.getCause(), instanceOf(CompositeRoutingException.class));

      CompositeRoutingException compositeRoutingException = (CompositeRoutingException) e.getCause();
      assertThat(compositeRoutingException.getMessage(), startsWith(exceptionMessageStart));

      List<Error> exceptions = compositeRoutingException.getErrors();
      assertThat(1, is(exceptions.size()));
      assertThat(exceptions.get(0).getCause(), instanceOf(exceptionType));
    }
  }

  @Test
  @Description("Only sa single thread is used to process all routes when configured with maxConcurrency=1.")
  public void sequentialProcessing() throws Exception {
    flowRunner("sequentialProcessing").withVariable("latch", new Latch()).run();
    assertThat(capturedThreads, hasSize(1));
  }

  @Test
  @Description("The result of all route failures and results are available via errorMessage in error-handler..")
  public void errorHandler() throws Exception {
    flowRunner("errorHandler").run();
  }

  @Test
  @Description("Variables set before route are conserved after router. Variables set in routes are merged and available after router.")
  public void variables() throws Exception {
    flowRunner("variables").run();
  }

  @Test
  @Description("By default routes are run concurrently and multiple threads are used.")
  public void concurrent() throws Exception {
    flowRunner("concurrent").withVariable("latch", new Latch()).run();
    assertThat(capturedThreads, hasSize(3));
  }

  @Test
  @Description("The resulting Map<String, Message result maintains the correct data-type for each Message.")
  public void returnsCorrectDataType() throws Exception {
    Message response = flowRunner("dataType").withMediaType(JSON).run().getMessage();
    assertThat(response.getPayload().getValue(), is(Matchers.instanceOf(Map.class)));
    Map<String, Message> messageList = (Map<String, Message>) response.getPayload().getValue();
    assertThat(messageList.size(), is(3));
    assertThat(messageList.get("0").getPayload().getDataType().getMediaType(), is(TEXT));
    assertThat(messageList.get("1").getPayload().getDataType().getMediaType(), is(ANY));
    assertThat(messageList.get("2").getPayload().getDataType().getMediaType(), is(ANY));
  }

  public static class ThreadCaptor extends AbstractComponent implements Processor {

    @Override
    public BaseEvent process(BaseEvent event) throws MuleException {
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
