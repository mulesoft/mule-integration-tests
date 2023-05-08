/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import static java.util.Arrays.asList;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.sameInstance;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test robustness of the Mule Runtime with misbehaving interceptors.
 */
@Features({@Feature(XML_SDK), @Feature(INTERCEPTION_API)})
@Story(COMPONENT_INTERCEPTION_STORY)
public class XmlSdkProcessorInterceptorFactoryFailingInterceptorsTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

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

  private static RuntimeException THROWN = new MuleRuntimeException(createStaticMessage("Expected exception in after()"));
  private final AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);

  public class FailingAfterInterceptorFactory implements ProcessorInterceptorFactory {

    public FailingAfterInterceptorFactory() {}

    @Override
    public ProcessorInterceptor get() {
      return new FailingAfterInterceptor();
    }

  }

  public class FailingAfterInterceptor implements ProcessorInterceptor {

    @Override
    public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        throw THROWN;
      }
    }
  }

  @Before
  public void before() {
    expectedError.expectErrorType("MULE", "UNKNOWN").expectCause(sameInstance(THROWN));
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_FailingAfterInterceptorFactory", new FailingAfterInterceptorFactory());

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(FailingAfterInterceptorFactory.class.getName()));

    return objects;
  }

  @Description("Smart Connector simple operation without parameters")
  @Test
  public void scOperation() throws Exception {
    flowRunner("scOperation").run();
  }

  @Description("Smart Connector simple operation with parameters")
  @Test
  public void scEchoOperation() throws Exception {
    final String variableValue = "echo message for the win";
    flowRunner("scEchoOperation").withVariable("variable", variableValue).run();
  }

  @Description("Smart Connector simple operation with parameters through flow-ref")
  @Test
  public void scEchoOperationFlowRef() throws Exception {
    final String variableValue = "echo message for the win";
    flowRunner("scEchoOperationFlowRef").withVariable("variable", variableValue).run();
  }

  @Description("Smart Connector that uses a Smart Connector operation without parameters")
  @Test
  public void scUsingScOperation() throws Exception {
    flowRunner("scUsingScOperation").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for errors in XML SDK operations")
  public void globalErrorHandlerScOperation() throws Exception {
    // Original error type kept regardless of interceptor failure
    expectedError.expectErrorType("MODULE-USING-CORE", "RAISED");
    flowRunner("scFailingOperation").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for errors in XML SDK operations, when referenced from flow ref")
  public void globalErrorHandlerScOperationFromFlowRef() throws Exception {
    // Original error type kept regardless of interceptor failure
    expectedError.expectErrorType("MODULE-USING-CORE", "RAISED");
    flowRunner("scFailingOperationFlowRef").run();
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
