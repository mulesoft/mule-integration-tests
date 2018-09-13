/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.util.Arrays.asList;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.TypedComponentIdentifier.ComponentType.SOURCE;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getActiveConnections;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.AfterWithCallbackInterceptor;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.AfterWithCallbackInterceptorFactory;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.EvaluatesExpressionInterceptorFactory;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptor;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptorFactory;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.InterceptionParameters;

import org.junit.After;
import org.junit.Test;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.function.BiConsumer;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactorySourcesTestCase extends AbstractIntegrationTestCase {

  private Flow flow;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory-source.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_AfterWithCallbackInterceptorFactory", new AfterWithCallbackInterceptorFactory());
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(false));
    objects.put("_EvaluatesExpressionInterceptorFactory", new EvaluatesExpressionInterceptorFactory());
    objects.put("_SourceCallbackInterceptor", new SourceCallbackInterceptorFactory());

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(AfterWithCallbackInterceptorFactory.class.getName(),
                                                         HasInjectedAttributesInterceptorFactory.class.getName(),
                                                         EvaluatesExpressionInterceptorFactory.class.getName(),
                                                         SourceCallbackInterceptorFactory.class.getName()));

    return objects;
  }

  @After
  public void after() throws MuleException {
    if (flow != null) {
      flow.stop();
    }

    getActiveConnections().clear();
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
    SourceCallbackInterceptor.interceptionParameters.clear();
    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
    };
    SourceCallbackInterceptor.afterCallback = (event, thrown) -> {
    };
  }

  // @Description("Logger, flow-ref and splitter components are intercepted in order and the parameters are correctly sent")
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
    assertThat(heisenbergSourceInterceptionParameter.getParameters().entrySet(), hasSize(8));
  }

  public static class SourceCallbackInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new SourceCallbackInterceptor();
    }

    @Override
    public boolean intercept(ComponentLocation location) {
      return SOURCE.equals(location.getComponentIdentifier().getType());
    }
  }

  public static class SourceCallbackInterceptor implements ProcessorInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> afterCallback = (event, thrown) -> {
    };

    static final List<InterceptionParameters> interceptionParameters = new LinkedList<>();

    @Override
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event) {
      interceptionParameters.add(new InterceptionParameters(location, parameters, event));
    }

    @Override
    public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      afterCallback.accept(event, thrown);
    }
  }

  protected void startFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.start();
  }

}
