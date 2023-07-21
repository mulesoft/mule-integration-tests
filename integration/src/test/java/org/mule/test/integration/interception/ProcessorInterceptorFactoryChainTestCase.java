/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.interception;

import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

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
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.VerboseExceptions;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactoryChainTestCase extends AbstractIntegrationTestCase {

  // Just to ensure the previous value is set after the test
  @ClassRule
  public static VerboseExceptions verboseExceptions = new VerboseExceptions(false);

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public DynamicPort wireMockPort = new DynamicPort("wireMockPort");

  @Rule
  public WireMockRule wireMock = new WireMockRule(wireMockConfig()
      .bindAddress("127.0.0.1")
      .port(wireMockPort.getNumber()));

  @Before
  public void setUp() {
    wireMock.stubFor(get(urlMatching("/404")).willReturn(aResponse().withStatus(404)));
    wireMock.stubFor(get(urlMatching("/418")).willReturn(aResponse().withStatus(418)));
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_AfterWithCallbackInterceptorFactory",
                new ProcessorInterceptorFactoryChainTestCase.AfterWithCallbackInterceptorFactory());
    objects.put("_ExecutesOperationInnerChainInterceptorFactory",
                new ExecutesTapPhonesOperationInnerChainInterceptorFactory(asList("operationWithChainAndCallback")));

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(ExecutesTapPhonesOperationInnerChainInterceptorFactory.class
                    .getName(), ProcessorInterceptorFactoryChainTestCase.AfterWithCallbackInterceptor.class.getName()));

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

  @Test
  @Issue("MULE-18099")
  public void operationWithChainAndCallback() throws Exception {
    AtomicBoolean afterCallbackCalled = new AtomicBoolean(false);
    ProcessorInterceptorFactoryTestCase.AfterWithCallbackInterceptor.callback =
        (interceptionEvent, throwable) -> afterCallbackCalled.getAndSet(true);
    try {
      flowRunner("operationWithChainAndCallback").run();
    } finally {
      assertThat(afterCallbackCalled.get(), is(true));
    }
  }

  public static class ExecutesTapPhonesOperationInnerChainInterceptorFactory implements ProcessorInterceptorFactory {

    private List<String> excludeRootLocations;

    public ExecutesTapPhonesOperationInnerChainInterceptorFactory(List<String> excludeRootLocations) {
      this.excludeRootLocations = excludeRootLocations;
    }

    @Override
    public boolean intercept(ComponentLocation location) {
      return "tap-phones".equals(location.getComponentIdentifier().getIdentifier().getName())
          && excludeRootLocations.stream().noneMatch(rootLocation -> location.getLocation().startsWith(rootLocation));
    }

    @Override
    public ProcessorInterceptor get() {
      return new ExecutesOperationInnerChainInterceptor();
    }
  }

  public static class AfterWithCallbackInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new ProcessorInterceptorFactoryTestCase.AfterWithCallbackInterceptor();
    }
  }

  public static class AfterWithCallbackInterceptor implements ProcessorInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> callback = (event, thrown) -> {
    };

    @Override
    public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      callback.accept(event, thrown);
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
