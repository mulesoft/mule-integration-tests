/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.lang.Math.random;
import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.FlowInterceptorFactory.FLOW_INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ExecutionEngineFeature.ExecutionEngineStory.BACKPRESSURE;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.FLOW_INTERCEPTION_STORY;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.FlowInterceptor;
import org.mule.runtime.api.interception.FlowInterceptorFactory;
import org.mule.runtime.api.interception.FlowInterceptorFactory.FlowInterceptorOrder;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.http.api.HttpService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
public class FlowInterceptorFactoryTestCase extends AbstractIntegrationTestCase {

  private static CountDownLatch latch;

  @Rule
  public ExpectedError expectedError = none();

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Inject
  @Named("withMaxConcurrency")
  public Flow withMaxConcurrency;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/flow-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_BeforeWithCallbackInterceptorFactory", new BeforeWithCallbackInterceptorFactory());
    objects.put("_AfterWithCallbackInterceptorFactory", new AfterWithCallbackInterceptorFactory());
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(false));
    objects.put("_EvaluatesExpressionInterceptorFactory", new EvaluatesExpressionInterceptorFactory());

    objects.put(FLOW_INTERCEPTORS_ORDER_REGISTRY_KEY,
                (FlowInterceptorOrder) () -> asList(BeforeWithCallbackInterceptorFactory.class.getName(),
                                                    AfterWithCallbackInterceptorFactory.class.getName(),
                                                    HasInjectedAttributesInterceptorFactory.class.getName(),
                                                    EvaluatesExpressionInterceptorFactory.class.getName()));

    return objects;
  }

  @Before
  public void before() {
    latch = new CountDownLatch(1);
  }

  @After
  public void after() {
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
    BeforeWithCallbackInterceptor.callback = event -> {
    };
    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
    };
  }

  @Test
  public void proceedFlowFailing() throws Exception {
    AtomicBoolean afterCallbackRun = new AtomicBoolean();

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      assertThat(thrown.isPresent(), is(true));
      final ErrorType errorType = event.getError().get().getErrorType();
      assertThat(errorType.getNamespace(), is("APP"));
      assertThat(errorType.getIdentifier(), is("ERROR"));
      afterCallbackRun.set(true);
    };

    flowRunner("flowFailing").runExpectingException(errorType("APP", "ERROR"));
    assertThat(afterCallbackRun.get(), is(true));
  }

  @Test
  public void proceedFlowHandleAndFail() throws Exception {
    AtomicBoolean afterCallbackRun = new AtomicBoolean();

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      assertThat(thrown.isPresent(), is(true));
      final ErrorType errorType = event.getError().get().getErrorType();
      assertThat(errorType.getNamespace(), is("APP"));
      assertThat(errorType.getIdentifier(), is("ERROR"));
      afterCallbackRun.set(true);
    };

    flowRunner("flowHandleAndFail").runExpectingException(errorType("APP", "ERROR"));
    assertThat(afterCallbackRun.get(), is(true));
  }

  @Test
  @Story(BACKPRESSURE)
  public void flowInterceptorAppliedAfterBackpressureCheck() throws MuleException, InterruptedException {
    final AtomicInteger beforeCounter = new AtomicInteger();

    BeforeWithCallbackInterceptor.callback = event -> beforeCounter.incrementAndGet();
    withMaxConcurrency.start();

    Thread.sleep(1500);

    assertThat(beforeCounter.get(), is(1));

    latch.countDown();

    probe(() -> beforeCounter.get() > 1);
  }

  public static class HasInjectedAttributesInterceptorFactory implements FlowInterceptorFactory {

    @Inject
    private MuleExpressionLanguage expressionEvaluator;

    @Inject
    private LockFactory lockFactory;

    @Inject
    private HttpService httpService;

    @Inject
    private ErrorTypeRepository errorTypeRepository;

    @Inject
    private SchedulerService schedulerService;

    @Inject
    private Registry registry;

    private final boolean mutateEventBefore;

    public HasInjectedAttributesInterceptorFactory(boolean mutateEventBefore) {
      this.mutateEventBefore = mutateEventBefore;
    }

    @Override
    public FlowInterceptor get() {
      return new HasInjectedAttributesInterceptor(expressionEvaluator, lockFactory, httpService, errorTypeRepository,
                                                  schedulerService, registry, mutateEventBefore);
    }
  }

  public static class HasInjectedAttributesInterceptor implements FlowInterceptor {

    static final List<InterceptionParameters> interceptionParameters = new LinkedList<>();

    private final MuleExpressionLanguage expressionEvaluator;
    private final LockFactory lockFactory;
    private final HttpService httpService;
    private final ErrorTypeRepository errorTypeRepository;
    private final SchedulerService schedulerService;
    private final Registry registry;

    private final boolean mutateEventBefore;

    public HasInjectedAttributesInterceptor(MuleExpressionLanguage expressionEvaluator, LockFactory lockFactory,
                                            HttpService httpService, ErrorTypeRepository errorTypeRepository,
                                            SchedulerService schedulerService, Registry registry, boolean mutateEventBefore) {
      this.expressionEvaluator = expressionEvaluator;
      this.lockFactory = lockFactory;
      this.httpService = httpService;
      this.errorTypeRepository = errorTypeRepository;
      this.schedulerService = schedulerService;
      this.registry = registry;
      this.mutateEventBefore = mutateEventBefore;
    }

    @Override
    public synchronized void before(String location, InterceptionEvent event) {
      assertThat(expressionEvaluator, not(nullValue()));
      assertThat(lockFactory, not(nullValue()));
      assertThat(httpService, not(nullValue()));
      assertThat(errorTypeRepository, not(nullValue()));
      assertThat(schedulerService, not(nullValue()));
      assertThat(registry, not(nullValue()));

      if (mutateEventBefore) {
        event.addVariable("mutated", random());
      }
    }

  }

  public static class InterceptionParameters {

    private final ComponentLocation location;
    private final Map<String, ProcessorParameterValue> parameters;
    private final InterceptionEvent event;

    public InterceptionParameters(ComponentLocation location, Map<String, ProcessorParameterValue> parameters,
                                  InterceptionEvent event) {
      this.location = location;
      this.parameters = parameters;
      this.event = event;
    }

    public ComponentLocation getLocation() {
      return location;
    }

    public Map<String, ProcessorParameterValue> getParameters() {
      return parameters;
    }

    public InterceptionEvent getEvent() {
      return event;
    }

    @Override
    public String toString() {
      return "InterceptionParameters{location: '" + location.getLocation() + "'; parameters: " + parameters + "}";
    }
  }

  public static class EvaluatesExpressionInterceptorFactory implements FlowInterceptorFactory {

    @Inject
    private MuleExpressionLanguage expressionEvaluator;

    public EvaluatesExpressionInterceptorFactory() {}

    @Override
    public FlowInterceptor get() {
      return new EvaluatesExpressionInterceptor(expressionEvaluator);
    }

    @Override
    public boolean intercept(String location) {
      return location.equals("expressionsInInterception");
    }
  }

  public static class EvaluatesExpressionInterceptor implements FlowInterceptor {

    private final MuleExpressionLanguage expressionEvaluator;

    public EvaluatesExpressionInterceptor(MuleExpressionLanguage expressionEvaluator) {
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public void before(String location, InterceptionEvent event) {
      try {
        expressionEvaluator.evaluate("vars.addedVar", event.asBindingContext());
      } catch (ExpressionRuntimeException e) {
        assertThat(e.getMessage(), containsString("Unable to resolve reference of addedVar"));
      }

      event.addVariable("addedVar", "value1");
      assertThat(expressionEvaluator.evaluate("vars.addedVar", event.asBindingContext()).getValue(), is("value1"));

      event.addVariable("addedVar", "value2");
      assertThat(expressionEvaluator.evaluate("vars.addedVar", event.asBindingContext()).getValue(), is("value2"));
    }

  }

  public static class BeforeWithCallbackInterceptorFactory implements FlowInterceptorFactory {

    @Override
    public FlowInterceptor get() {
      return new BeforeWithCallbackInterceptor();
    }
  }

  public static class BeforeWithCallbackInterceptor implements FlowInterceptor {

    static Consumer<InterceptionEvent> callback = event -> {
    };

    @Override
    public void before(String location, InterceptionEvent event) {
      callback.accept(event);
    }
  }

  public static class AfterWithCallbackInterceptorFactory implements FlowInterceptorFactory {

    @Override
    public FlowInterceptor get() {
      return new AfterWithCallbackInterceptor();
    }
  }

  public static class AfterWithCallbackInterceptor implements FlowInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> callback = (event, thrown) -> {
    };

    @Override
    public void after(String location, InterceptionEvent event, Optional<Throwable> thrown) {
      callback.accept(event, thrown);
    }
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }

  public static Object await(Object payload) {
    try {
      latch.await();
    } catch (InterruptedException e) {
      currentThread().interrupt();
    }
    return payload;

  }
}
