/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getActiveConnections;
import static org.mule.test.heisenberg.extension.HeisenbergSource.resetHeisenbergSource;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.api.interception.SourceInterceptor;
import org.mule.runtime.api.interception.SourceInterceptorFactory;
import org.mule.runtime.api.message.ErrorType;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.InterceptionParameters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class SourceInterceptorFactoryTestCase extends AbstractIntegrationTestCase {

  private Flow flow;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/source-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    objects.put("_SourceCallbackInterceptor", new SourceCallbackInterceptorFactory());
    return objects;
  }

  @After
  public void after() throws MuleException {
    if (flow != null) {
      flow.stop();
    }

    getActiveConnections().clear();
    SourceCallbackInterceptor.interceptionParameters.clear();
    SourceCallbackInterceptor.afterCallback = (event, thrown) -> {
    };

    resetHeisenbergSource();
  }

  @Test
  public void sourceIntercepted() throws Exception {
    startFlow("sourceIntercepted");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterCallback = (event, thrown) -> {
      if (!thrown.isPresent()) {
        afterCalledLatch.countDown();
      }
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(0);
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("fail"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("config-ref"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("initialBatchNumber"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("payment"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("frequency"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("propagateError"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("corePoolSize"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("onCapacityOverload"));
  }

  @Test
  public void sourceInterceptedAfterTerminated() throws Exception {
    startFlow("sourceIntercepted");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterTerminated = (event, thrown) -> {
      if (!thrown.isPresent()) {
        afterCalledLatch.countDown();
      }
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(0);
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("fail"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("config-ref"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("initialBatchNumber"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("payment"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("frequency"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("propagateError"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("corePoolSize"));
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters(), hasKey("onCapacityOverload"));
  }

  @Test
  public void sourceErrorIntercepted() throws Exception {
    startFlow("sourceErrorIntercepted");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterCallback = (event, thrown) -> {
      thrown.ifPresent(t -> afterCalledLatch.countDown());
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(interceptionParameters.size() - 1);
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
  }

  @Test
  public void sourceErrorInterceptedAfterTerminated() throws Exception {
    startFlow("sourceErrorIntercepted");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterTerminated = (event, thrown) -> {
      thrown.ifPresent(t -> afterCalledLatch.countDown());
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(interceptionParameters.size() - 1);
    assertThat(heisenbergSourceInterceptionParameter.toString(),
               heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
  }

  @Test
  public void sourceInterceptedWithFailingProcessor() throws Exception {
    startFlow("sourceInterceptedWithFailingProcessor");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterCallback = (event, thrown) -> {
      if (event.getError().isPresent()) {
        ErrorType errorType = event.getError().get().getErrorType();
        assertThat(errorType.getNamespace(), equalTo("APP"));
        assertThat(errorType.getIdentifier(), equalTo("RAISED"));
        afterCalledLatch.countDown();
      }
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(0);
    assertThat(heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("fail"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("config-ref"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("initialBatchNumber"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("payment"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("frequency"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("propagateError"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("corePoolSize"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("onCapacityOverload"));
  }

  @Test
  public void sourceInterceptedAfterTerminatedWithFailingProcessor() throws Exception {
    startFlow("sourceInterceptedWithFailingProcessor");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterTerminated = (event, thrown) -> {
      if (event.getError().isPresent()) {
        ErrorType errorType = event.getError().get().getErrorType();
        assertThat(errorType.getNamespace(), equalTo("APP"));
        assertThat(errorType.getIdentifier(), equalTo("RAISED"));
        afterCalledLatch.countDown();
      }
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(0);
    assertThat(heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("fail"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("config-ref"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("initialBatchNumber"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("payment"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("frequency"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("propagateError"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("corePoolSize"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("onCapacityOverload"));
  }

  @Test
  public void sourceInterceptedAfterTerminatedWithFailingReferencedFlow() throws Exception {
    startFlow("sourceInterceptedWithFailingReferencedFlow");

    CountDownLatch afterCalledLatch = new CountDownLatch(1);

    SourceCallbackInterceptor.afterTerminated = (event, thrown) -> {
      if (event.getError().isPresent()) {
        ErrorType errorType = event.getError().get().getErrorType();
        assertThat(errorType.getNamespace(), equalTo("APP"));
        assertThat(errorType.getIdentifier(), equalTo("RAISED"));
        afterCalledLatch.countDown();
      }
    };

    assertThat(afterCalledLatch.await(RECEIVE_TIMEOUT, MILLISECONDS), is(true));
    List<InterceptionParameters> interceptionParameters = SourceCallbackInterceptor.interceptionParameters;

    assertThat(interceptionParameters, hasSize(greaterThanOrEqualTo(1)));
    InterceptionParameters heisenbergSourceInterceptionParameter = interceptionParameters.get(0);
    assertThat(heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("fail"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("config-ref"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("initialBatchNumber"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("payment"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("frequency"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("propagateError"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("corePoolSize"));
    assertThat(heisenbergSourceInterceptionParameter.getParameters(), hasKey("onCapacityOverload"));
  }

  public static class SourceCallbackInterceptorFactory implements SourceInterceptorFactory {

    @Override
    public SourceInterceptor get() {
      return new SourceCallbackInterceptor();
    }

  }

  public static class SourceCallbackInterceptor implements SourceInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> afterCallback = (event, thrown) -> {
    };

    static BiConsumer<InterceptionEvent, Optional<Throwable>> afterTerminated = (event, thrown) -> {
    };

    static final List<InterceptionParameters> interceptionParameters = new LinkedList<>();

    @Override
    public void beforeCallback(ComponentLocation location, Map<String, ProcessorParameterValue> parameters,
                               InterceptionEvent event) {
      interceptionParameters.add(new InterceptionParameters(location, parameters, event));
    }

    @Override
    public void afterCallback(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      afterCallback.accept(event, thrown);
    }

    @Override
    public void afterTerminated(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      afterTerminated.accept(event, thrown);
    }
  }

  protected void startFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.start();
  }

}
