/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.extension.api.runtime.route.Chain;
import org.mule.runtime.module.extension.api.runtime.privileged.EventedResult;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactoryChainTestCase extends AbstractIntegrationTestCase {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static VerboseExceptions verboseExceptions = new VerboseExceptions(false);

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_ExecutesOperationInnerChainInterceptorFactory", new ExecutesOperationInnerChainInterceptorFactory());

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(ExecutesOperationInnerChainInterceptorFactory.class.getName()));

    return objects;
  }

  @Test
  public void operationWithChain() throws Exception {
    flowRunner("operationWithChain").run();
  }

  @Test
  @Issue("MULE-18501")
  public void operationWithChainFailingLogsCorrectly() throws Exception {
    flowRunner("operationWithChainFailing").run();
  }

  public static class ExecutesOperationInnerChainInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public boolean intercept(ComponentLocation location) {
      return "tap-phones".equals(location.getComponentIdentifier().getIdentifier().getName());
    }

    @Override
    public ProcessorInterceptor get() {
      return new ExecutesOperationInnerChainInterceptor();
    }
  }

  public static class ExecutesOperationInnerChainInterceptor implements ProcessorInterceptor {

    @Override
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event) {
      final ProcessorParameterValue operationsParam = parameters.get("operations");

      final CountDownLatch latch = new CountDownLatch(1);
      AtomicReference<Throwable> thrownByChain = new AtomicReference<>();

      ((Chain) (operationsParam.resolveValue())).process(EventedResult.builder(event.getMessage()).build(),
                                                         result -> latch.countDown(),
                                                         (error, result) -> {
                                                           thrownByChain.set(error);
                                                           latch.countDown();
                                                         });

      try {
        latch.await();
      } catch (InterruptedException e) {
        currentThread().interrupt();
        throw new RuntimeException(e);
      }

      if (thrownByChain.get() != null) {
        throw new RuntimeException(thrownByChain.get());
      }
    }

    @Override
    public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
                                                       Map<String, ProcessorParameterValue> parameters, InterceptionEvent event,
                                                       InterceptionAction action) {
      return action.skip();
    }
  }

}
