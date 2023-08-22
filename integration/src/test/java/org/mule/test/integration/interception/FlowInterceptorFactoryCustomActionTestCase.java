/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.FLOW_INTERCEPTION_STORY;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.interception.FlowInterceptor;
import org.mule.runtime.api.interception.FlowInterceptorFactory;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptor;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(FLOW_INTERCEPTION_STORY)
public class FlowInterceptorFactoryCustomActionTestCase extends AbstractIntegrationTestCase {

  private static AtomicInteger counter = new AtomicInteger();

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Inject
  @Named("counting")
  public Flow counting;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/flow-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_CustomActionInterceptorFactory", new CustomActionInterceptorFactory());

    return objects;
  }

  @Before
  public void before() {
    counter = new AtomicInteger();
    CustomActionInterceptor.actioner = (flowName, action) -> action.proceed();
  }

  @After
  public void after() {
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
  }

  @Test
  public void proceedFlowRef() throws Exception {
    expectedError.expectErrorType("APP", "ERROR");
    flowRunner("flowRefOuter").run();
  }

  @Test
  public void proceedDataWeaveLookup() throws Exception {
    expectedError.expectErrorType("MULE", "EXPRESSION");
    flowRunner("lookupOuter").run();
  }

  @Test
  public void skipFlowRef() throws Exception {
    skipFlow("flowFailing");
    flowRunner("flowRefOuter").run();
  }

  @Test
  public void skipDataWeaveLookup() throws Exception {
    skipFlow("flowFailing");
    flowRunner("lookupOuter").run();
  }

  @Test
  public void skipFlowWithSource() throws Exception {
    skipFlow("counting");

    counting.start();
    Thread.sleep(RECEIVE_TIMEOUT);
    assertThat(counter.get(), is(0));
  }

  private void skipFlow(String skipFlowName) {
    CustomActionInterceptor.actioner = (flowName, action) -> {
      if (skipFlowName.equals(flowName)) {
        return action.skip();
      } else {
        return action.proceed();
      }
    };
  }

  @Test
  public void interceptorFailAction() throws Exception {
    final IllegalStateException expected = new IllegalStateException();

    CustomActionInterceptor.actioner = (flowName, action) -> {
      if ("counting".equals(flowName)) {
        return action.fail(expected);
      } else {
        return action.proceed();
      }
    };

    flowRunner("counting").runExpectingException(sameInstance(expected));
  }

  @Test
  public void interceptorFailHandledBySource() throws Exception {
    final IllegalStateException expected = new IllegalStateException();
    CustomActionInterceptor.actioner = (flowName, action) -> {
      if ("countingHttpServer".equals(flowName)) {
        return action.fail(expected);
      } else {
        return action.proceed();
      }
    };
    flowRunner("countingHttpClient").run();
  }

  public static class CustomActionInterceptorFactory implements FlowInterceptorFactory {

    @Override
    public FlowInterceptor get() {
      return new CustomActionInterceptor();
    }
  }

  public static class CustomActionInterceptor implements FlowInterceptor {

    private static BiFunction<String, InterceptionAction, CompletableFuture<InterceptionEvent>> actioner =
        (flowName, action) -> action.proceed();

    @Override
    public CompletableFuture<InterceptionEvent> around(String flowName,
                                                       InterceptionEvent event, InterceptionAction action) {
      return actioner.apply(flowName, action);
    }
  }

  public static Object count(Object payload) {
    counter.incrementAndGet();
    return payload;
  }
}
