/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getActiveConnections;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getConnects;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getDisconnects;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.heisenberg.extension.HeisenbergConnection;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptor;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptorFactory;
import org.mule.test.integration.interception.ProcessorInterceptorFactoryTestCase.InterceptionParameters;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
@RunnerDelegateTo(Parameterized.class)
public class ProcessorInterceptorFactoryCustomActionTestCase extends AbstractIntegrationTestCase {


  @Rule
  public ExpectedError expectedError = none();

  private final boolean mutateEventBefore;

  public ProcessorInterceptorFactoryCustomActionTestCase(boolean mutateEventBefore) {
    this.mutateEventBefore = mutateEventBefore;
  }

  @Parameters(name = "{0}")
  public static Collection<Object> data() {
    return asList(true, false);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_CustomActionInterceptorFactory", new CustomActionInterceptorFactory());
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(mutateEventBefore));

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(CustomActionInterceptorFactory.class.getName(),
                                                         HasInjectedAttributesInterceptorFactory.class.getName()));

    return objects;
  }

  @Before
  public void before() {
    getActiveConnections().clear();
    CustomActionInterceptor.actioner = action -> action.proceed();
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
  }

  @Test
  @Description("The connection was fetched on the interceptor, and released by the interceptor")
  public void resolvedConnectionParamSkips() throws Exception {
    int connectsBefore = getConnects();
    int disconnectsBefore = getDisconnects();

    CustomActionInterceptor.actioner = action -> action.skip();

    flowRunner("callSaul").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(1));

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(), containsInAnyOrder("targetValue", "config-ref", "connection"));
    assertThat(killInterceptionParameter.getParameters().get("config-ref").resolveValue(), is("heisenberg"));
    assertThat(killInterceptionParameter.getParameters().get("connection").resolveValue(),
               is(instanceOf(HeisenbergConnection.class)));

    assertThat(getActiveConnections(), empty());
    assertThat(getConnects() - connectsBefore, is(mutateEventBefore ? 2 : 1));
    assertThat(getDisconnects() - disconnectsBefore, is(mutateEventBefore ? 2 : 1));
  }

  @Test
  @Description("The connection was fetched on the interceptor, and released by the interceptor")
  public void resolvedConnectionParamFails() throws Exception {
    int connectsBefore = getConnects();
    int disconnectsBefore = getDisconnects();

    CustomActionInterceptor.actioner = action -> action.fail(new RuntimeException());

    try {
      flowRunner("callSaul").run();
      fail("Expected an exception. Refer to ReactiveInterceptorAdapterTestCase");
    } catch (Exception e) {
      // expected
    } finally {
      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

      assertThat(killInterceptionParameter.getParameters().keySet(),
                 containsInAnyOrder("targetValue", "config-ref", "connection"));
      assertThat(killInterceptionParameter.getParameters().get("config-ref").resolveValue(), is("heisenberg"));
      assertThat(killInterceptionParameter.getParameters().get("connection").resolveValue(),
                 is(instanceOf(HeisenbergConnection.class)));

      assertThat(getActiveConnections(), empty());
      assertThat(getConnects() - connectsBefore, is(mutateEventBefore ? 2 : 1));
      assertThat(getDisconnects() - disconnectsBefore, is(mutateEventBefore ? 2 : 1));

      // the 2nd one is for the global error handler, it is tested separately
    }
  }

  @Test
  @Description("The connection was fetched on the interceptor, and the operation uses the connection obtained there rather then fetching it again")
  public void resolvedConnectionParam() throws Exception {
    int connectsBefore = getConnects();
    int disconnectsBefore = getDisconnects();

    flowRunner("callSaul").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(1));

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(), containsInAnyOrder("targetValue", "config-ref", "connection"));
    assertThat(killInterceptionParameter.getParameters().get("config-ref").resolveValue(), is("heisenberg"));
    assertThat(killInterceptionParameter.getParameters().get("connection").resolveValue(),
               is(instanceOf(HeisenbergConnection.class)));

    assertThat(getActiveConnections(), empty());
    assertThat(getConnects() - connectsBefore, is(mutateEventBefore ? 2 : 1));
    assertThat(getDisconnects() - disconnectsBefore, is(mutateEventBefore ? 2 : 1));
  }

  public static class CustomActionInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new CustomActionInterceptor();
    }
  }

  public static class CustomActionInterceptor implements ProcessorInterceptor {

    private static Function<InterceptionAction, CompletableFuture<InterceptionEvent>> actioner = action -> action.proceed();

    @Override
    public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
                                                       Map<String, ProcessorParameterValue> parameters,
                                                       InterceptionEvent event, InterceptionAction action) {
      return actioner.apply(action);
    }
  }
}
