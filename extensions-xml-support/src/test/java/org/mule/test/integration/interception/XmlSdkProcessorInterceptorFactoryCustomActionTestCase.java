/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.interception;

import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.util.Arrays.asList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;
import org.mule.test.integration.interception.XmlSdkProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptor;
import org.mule.test.integration.interception.XmlSdkProcessorInterceptorFactoryTestCase.HasInjectedAttributesInterceptorFactory;
import org.mule.test.integration.interception.XmlSdkProcessorInterceptorFactoryTestCase.InterceptionParameters;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Function;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@Features({@Feature(XML_SDK), @Feature(INTERCEPTION_API)})
@Story(COMPONENT_INTERCEPTION_STORY)
@RunnerDelegateTo(Parameterized.class)
public class XmlSdkProcessorInterceptorFactoryCustomActionTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {


  @Rule
  public ExpectedError expectedError = none();

  private final boolean mutateEventBefore;

  public XmlSdkProcessorInterceptorFactoryCustomActionTestCase(boolean mutateEventBefore) {
    this.mutateEventBefore = mutateEventBefore;
  }

  @Parameters(name = "{0}")
  public static Collection<Object> data() {
    return asList(true, false);
  }

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

    objects.put("_CustomActionInterceptorFactory", new CustomActionInterceptorFactory());
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(mutateEventBefore));

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(CustomActionInterceptorFactory.class.getName(),
                                                         HasInjectedAttributesInterceptorFactory.class.getName()));

    return objects;
  }

  @Before
  public void before() {
    CustomActionInterceptor.actioner = action -> action.proceed();
  }

  @After
  public void after() {
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
  }

  @Description("Smart Connector simple operation without parameters")
  @Test
  public void scOperation() throws Exception {
    flowRunner("scOperation").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(2));

    InterceptionParameters moduleOperationChain = interceptionParameters.get(0);
    InterceptionParameters setPayloadOperation = interceptionParameters.get(1);

    assertThat(moduleOperationChain.getParameters().keySet(),
               containsInAnyOrder("doc:name", "targetValue"));
    assertThat(moduleOperationChain.getParameters().get("doc:name").resolveValue(), is("mySCName"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is("Wubba Lubba Dub Dub"));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
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
