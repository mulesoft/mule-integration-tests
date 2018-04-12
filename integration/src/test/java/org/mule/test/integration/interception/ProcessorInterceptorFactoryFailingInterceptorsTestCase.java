/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.sameInstance;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

/**
 * Test robustness of the Mule Runtime with misbehaving interceptors.
 */
@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactoryFailingInterceptorsTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  private static RuntimeException THROWN = new MuleRuntimeException(createStaticMessage("Expected exception in after()"));
  private AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);

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

  @Description("Logger, flow-ref and splitter components are intercepted in order and the parameters are correctly sent")
  @Test
  public void injection() throws Exception {
    List<Object> payload = new ArrayList<>();
    flowRunner("injectionInterceptionTest").withPayload(payload).run();

  }

  @Test
  public void operationParameters() throws Exception {
    flowRunner("killFromPayload").withPayload("T-1000").withVariable("goodbye", "Hasta la vista, baby").run();

  }

  @Test
  public void resolvedConfigOperationParameters() throws Exception {
    flowRunner("die").run();

  }

  @Test
  public void resolvedComplexParametersOperationParameters() throws Exception {
    flowRunner("killWithCustomMessage").withVariable("goodbye", "Hasta la vista, baby").run();

  }

  @Test
  @Description("The errorType set by an operation is preserved if an interceptor is applied")
  public void failingOperationErrorTypePreserved() throws Exception {
    flowRunner("callGusFring").run();
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
  @Description("Errors in sub-flows are handled correctly")
  public void failingSubFlow() throws Exception {
    flowRunner("flowWithFailingSubFlowRef").run();
  }

  @Test
  @Description("Processors in error handlers are intercepted correctly")
  public void errorHandler() throws Exception {
    flowRunner("flowFailingWithErrorHandler").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly")
  public void globalErrorHandler() throws Exception {
    flowRunner("flowFailing").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly when raise-error is used")
  public void globalErrorHandlerRaise() throws Exception {
    // Original error type kept regardless of interceptor failure
    expectedError.expectErrorType("MULE", "CONNECTIVITY");
    flowRunner("flowRaise").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for errors in operations")
  public void globalErrorHandlerOperation() throws Exception {
    // Original error type kept regardless of interceptor failure
    expectedError.expectErrorType("HEISENBERG", "HEALTH");
    flowRunner("flowFailingOperation").run();
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for an unknown status code errors in http request")
  public void globalErrorHandlerUnknownStatusCodeHttpRequest() throws Exception {
    flowRunner("flowUnknownStatusCodeHttpRequest").run();
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

  @Test
  @Description("Processors in global error handlers are intercepted correctly when error is in referenced flow")
  public void globalErrorHandlerWithFlowRef() throws Exception {
    flowRunner("flowWithFailingFlowRef").run();
  }

}
