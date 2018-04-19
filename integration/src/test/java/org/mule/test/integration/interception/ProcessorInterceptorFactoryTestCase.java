/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.lang.Math.random;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.heisenberg.extension.HeisenbergConnectionProvider.getActiveConnections;
import static org.mule.test.heisenberg.extension.HeisenbergOperations.CALL_GUS_MESSAGE;

import org.mule.extension.http.api.request.validator.ResponseValidatorException;
import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.api.exception.FunctionalTestException;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.ErrorTypeRepository;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory.ProcessorInterceptorOrder;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.http.api.HttpService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.heisenberg.extension.HeisenbergExtension;
import org.mule.test.heisenberg.extension.exception.HeisenbergException;
import org.mule.test.heisenberg.extension.model.KillParameters;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTERCEPTION_API)
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactoryTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();

    objects.put("_AfterWithCallbackInterceptorFactory", new AfterWithCallbackInterceptorFactory());
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(false));
    objects.put("_EvaluatesExpressionInterceptorFactory", new EvaluatesExpressionInterceptorFactory());

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(AfterWithCallbackInterceptorFactory.class.getName(),
                                                         HasInjectedAttributesInterceptorFactory.class.getName(),
                                                         EvaluatesExpressionInterceptorFactory.class.getName()));

    return objects;
  }

  @Before
  public void before() {
    getActiveConnections().clear();
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
    };
  }

  @Description("Logger, flow-ref and splitter components are intercepted in order and the parameters are correctly sent")
  @Test
  public void injection() throws Exception {
    List<Object> payload = new ArrayList<>();
    flowRunner("injectionInterceptionTest").withPayload(payload).run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    InterceptionParameters loggerInterceptionParameter = interceptionParameters.get(0);
    InterceptionParameters flowRefInterceptionParameter = interceptionParameters.get(1);
    InterceptionParameters splitterInterceptionParameter = interceptionParameters.get(2);

    assertThat(loggerInterceptionParameter.getParameters().isEmpty(), is(true));
    assertThat(flowRefInterceptionParameter.getParameters().get("name").resolveValue(), is("anotherFlow"));
    assertThat(splitterInterceptionParameter.getParameters().get("expression").resolveValue(), is(payload));
  }

  @Test
  public void operationParameters() throws Exception {
    flowRunner("killFromPayload").withPayload("T-1000").withVariable("goodbye", "Hasta la vista, baby").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(1));

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(), containsInAnyOrder("targetValue", "victim", "goodbyeMessage"));
    assertThat(killInterceptionParameter.getParameters().get("victim").resolveValue(), is("T-1000"));
    assertThat(killInterceptionParameter.getParameters().get("goodbyeMessage").resolveValue(), is("Hasta la vista, baby"));
  }

  @Test
  public void resolvedConfigOperationParameters() throws Exception {
    flowRunner("die").run();

    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(1));
    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;

    InterceptionParameters dieInterceptionParameter = interceptionParameters.get(0);

    assertThat(dieInterceptionParameter.getParameters().keySet(), containsInAnyOrder("config-ref", "config"));
    final Object config = dieInterceptionParameter.getParameters().get("config").resolveValue();
    assertThat(config, instanceOf(HeisenbergExtension.class));
    assertThat(((HeisenbergExtension) config).getConfigName(), is("heisenberg"));

    final Object configRef = dieInterceptionParameter.getParameters().get("config-ref").resolveValue();
    assertThat(configRef, is("heisenberg"));
  }

  @Test
  public void operationThatUsesExtensionsClientInternally() throws Exception {
    assertThat(flowRunner("executeKillWithClient").run().getMessage().getPayload().getValue().toString(),
               is("Now he sleeps with the fishes."));

    assertThat(HasInjectedAttributesInterceptor.interceptionParameters.size(), is(1));
  }

  @Test
  public void resolvedComplexParametersOperationParameters() throws Exception {
    flowRunner("killWithCustomMessage").withVariable("goodbye", "Hasta la vista, baby").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(1));

    InterceptionParameters killInterceptionParameter = interceptionParameters.get(0);

    assertThat(killInterceptionParameter.getParameters().keySet(),
               containsInAnyOrder("targetValue", "victim", "goodbyeMessage", "killParameters"));
    assertThat(killInterceptionParameter.getParameters().get("victim").resolveValue(), is("T-1000"));
    assertThat(killInterceptionParameter.getParameters().get("goodbyeMessage").resolveValue(), is("Hasta la vista, baby"));
    assertThat(killInterceptionParameter.getParameters().get("killParameters").resolveValue(),
               is(instanceOf(KillParameters.class)));
  }

  @Test
  @Description("The errorType set by an operation is preserved if an interceptor is applied")
  public void failingOperationErrorTypePreserved() throws Exception {
    AtomicBoolean afterCallbackCalled = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      assertThat(thrown.get(), instanceOf(ConnectionException.class));
      assertThat(thrown.get().getCause(), instanceOf(HeisenbergException.class));
      assertThat(thrown.get().getMessage(), endsWith(CALL_GUS_MESSAGE));

      assertThat(event.getError().get().getErrorType(), errorType("HEISENBERG", "CONNECTIVITY"));

      afterCallbackCalled.set(true);
    };

    expectedError.expectErrorType("HEISENBERG", "CONNECTIVITY");
    try {
      flowRunner("callGusFring").run();
    } finally {
      assertThat(afterCallbackCalled.get(), is(true));
    }
  }

  @Test
  public void expressionsInInterception() throws Exception {
    assertThat(flowRunner("expressionsInInterception").run().getVariables().get("addedVar").getValue(), is("value2"));
  }

  @Description("Smart Connector simple operation without parameters")
  @Test
  public void scOperation() throws Exception {
    flowRunner("scOperation").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(2));

    InterceptionParameters moduleOperationChain = interceptionParameters.get(0);
    InterceptionParameters setPayloadOperation = interceptionParameters.get(1);

    assertThat(moduleOperationChain.getParameters().keySet(), containsInAnyOrder("moduleName", "moduleOperation"));
    assertThat(moduleOperationChain.getParameters().get("moduleName").resolveValue(), is("module-using-core"));
    assertThat(moduleOperationChain.getParameters().get("moduleOperation").resolveValue(), is("set-payload-hardcoded"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is("Wubba Lubba Dub Dub"));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
  }

  @Description("Smart Connector simple operation with parameters")
  @Test
  public void scEchoOperation() throws Exception {
    final String variableValue = "echo message for the win";
    flowRunner("scEchoOperation").withVariable("variable", variableValue).run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(2));

    InterceptionParameters moduleOperationChain = interceptionParameters.get(0);
    InterceptionParameters setPayloadOperation = interceptionParameters.get(1);

    assertThat(moduleOperationChain.getParameters().keySet(), containsInAnyOrder("moduleName", "moduleOperation"));
    assertThat(moduleOperationChain.getParameters().get("moduleName").resolveValue(), is("module-using-core"));
    assertThat(moduleOperationChain.getParameters().get("moduleOperation").resolveValue(), is("echo-set-payload"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").providedValue(), is("#[vars.echoMessage]"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is(variableValue));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
  }

  @Description("Smart Connector simple operation with parameters through flow-ref")
  @Test
  public void scEchoOperationFlowRef() throws Exception {
    final String variableValue = "echo message for the win";
    flowRunner("scEchoOperationFlowRef").withVariable("variable", variableValue).run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    InterceptionParameters flowRef = interceptionParameters.get(0);
    InterceptionParameters moduleOperationChain = interceptionParameters.get(1);
    InterceptionParameters setPayloadOperation = interceptionParameters.get(2);

    assertThat(flowRef.getParameters().keySet(), containsInAnyOrder("name", "targetValue"));
    assertThat(flowRef.getParameters().get("name").resolveValue(), is("scEchoOperation"));

    assertThat(moduleOperationChain.getParameters().keySet(), containsInAnyOrder("moduleName", "moduleOperation"));
    assertThat(moduleOperationChain.getParameters().get("moduleName").resolveValue(), is("module-using-core"));
    assertThat(moduleOperationChain.getParameters().get("moduleOperation").resolveValue(), is("echo-set-payload"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").providedValue(), is("#[vars.echoMessage]"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is(variableValue));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
  }

  @Description("Smart Connector that uses a Smart Connector operation without parameters")
  @Test
  public void scUsingScOperation() throws Exception {
    flowRunner("scUsingScOperation").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    InterceptionParameters proxyModuleOperationChain = interceptionParameters.get(0);
    InterceptionParameters innerModuleOperationChain = interceptionParameters.get(1);
    InterceptionParameters setPayloadOperation = interceptionParameters.get(2);

    assertThat(proxyModuleOperationChain.getParameters().keySet(), containsInAnyOrder("moduleName", "moduleOperation"));
    assertThat(proxyModuleOperationChain.getParameters().get("moduleName").resolveValue(), is("module-using-sc"));
    assertThat(proxyModuleOperationChain.getParameters().get("moduleOperation").resolveValue(),
               is("proxy-set-payload-hardcoded"));

    assertThat(innerModuleOperationChain.getParameters().keySet(), containsInAnyOrder("moduleName", "moduleOperation"));
    assertThat(innerModuleOperationChain.getParameters().get("moduleName").resolveValue(), is("module-using-core"));
    assertThat(innerModuleOperationChain.getParameters().get("moduleOperation").resolveValue(), is("set-payload-hardcoded"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is("Wubba Lubba Dub Dub"));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
  }

  @Test
  @Description("Errors in sub-flows are handled correctly")
  public void failingSubFlow() throws Exception {
    expectedError.expectCause(instanceOf(FunctionalTestException.class));

    try {
      flowRunner("flowWithFailingSubFlowRef").run();
    } finally {
      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(3));

      InterceptionParameters flowRefParameter = interceptionParameters.get(0);

      assertThat(flowRefParameter.getParameters().keySet(), containsInAnyOrder("name", "targetValue"));
      assertThat(flowRefParameter.getParameters().get("name").resolveValue(), is("failing-sub-flow"));

      InterceptionParameters failParameter = interceptionParameters.get(1);

      assertThat(failParameter.getParameters().keySet(), containsInAnyOrder("throwException"));
      assertThat(failParameter.getParameters().get("throwException").resolveValue(), is("true"));

      // the 3rd one is for the global error handler, it is tested separately
    }
  }

  @Test
  @Description("Processors in error handlers are intercepted correctly")
  public void errorHandler() throws Exception {
    expectedError.expectCause(instanceOf(FunctionalTestException.class));

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(FunctionalTestException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("flowFailingWithErrorHandler").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(1);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(),
                 is("flowFailingWithErrorHandler/errorHandler/0/processors/0"));
    }
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly")
  public void globalErrorHandler() throws Exception {
    expectedError.expectCause(instanceOf(FunctionalTestException.class));

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(FunctionalTestException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("flowFailing").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(1);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(), is("globalErrorHandler/0/processors/0"));
    }
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly when raise-error is used")
  public void globalErrorHandlerRaise() throws Exception {
    expectedError.expectCause(instanceOf(DefaultMuleException.class));

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(DefaultMuleException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("flowRaise").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(1);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(), is("globalErrorHandler/0/processors/0"));
    }
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for errors in operations")
  public void globalErrorHandlerOperation() throws Exception {
    expectedError.expectCause(instanceOf(HeisenbergException.class));

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(HeisenbergException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("flowFailingOperation").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(1);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(), is("globalErrorHandler/0/processors/0"));
    }
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for an unknown status code errors in http request")
  public void globalErrorHandlerUnknownStatusCodeHttpRequest() throws Exception {
    expectedError.expectCause(instanceOf(ResponseValidatorException.class));

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(ResponseValidatorException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("flowUnknownStatusCodeHttpRequest").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(2));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(1);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(), is("globalErrorHandler/0/processors/0"));
    }
  }

  @Test
  public void loggerWithTemplate() throws Exception {
    flowRunner("loggerWithTemplate").withVariable("v1", "value").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(1));

    InterceptionParameters param = interceptionParameters.get(0);
    assertThat(param.parameters.get("message").resolveValue(), is("Logging value"));
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly for errors in XML SDK operations")
  public void globalErrorHandlerScOperation() throws Exception {
    expectedError.expectErrorType("MODULE-USING-CORE", "RAISED");

    AtomicBoolean afterCallbackCalledForFailingMP = new AtomicBoolean(false);
    AtomicBoolean afterCallbackCalledForErrorHandlingMp = new AtomicBoolean(false);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      if (!afterCallbackCalledForFailingMP.getAndSet(true)) {
        assertThat(thrown.get(), instanceOf(DefaultMuleException.class));
      } else {
        afterCallbackCalledForErrorHandlingMp.set(true);
      }
    };

    try {
      flowRunner("scFailingOperation").run();
    } finally {
      assertThat(afterCallbackCalledForFailingMP.get(), is(true));
      assertThat(afterCallbackCalledForErrorHandlingMp.get(), is(true));

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(3));

      InterceptionParameters mpInGlobalErrorHandler = interceptionParameters.get(2);
      assertThat(mpInGlobalErrorHandler.getLocation().getLocation(), is("globalErrorHandler/0/processors/0"));
    }
  }

  @Test
  @Description("Processors in global error handlers are intercepted correctly when error is in referenced flow")
  public void globalErrorHandlerWithFlowRef() throws Exception {
    expectedError.expectCause(instanceOf(FunctionalTestException.class));

    AtomicInteger afters = new AtomicInteger(0);

    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
      afters.incrementAndGet();
    };

    try {
      flowRunner("flowWithFailingFlowRef").run();
    } finally {
      // The MP in the global error handler is ran twice, once for the called flow and another for tha caller flow.

      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters.stream().map(ip -> ip.getLocation().getLocation()).collect(toList()).toString(),
                 interceptionParameters, hasSize(4));

      assertThat(afters.get(), is(4));
    }
  }

  public static class HasInjectedAttributesInterceptorFactory implements ProcessorInterceptorFactory {

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

    private boolean mutateEventBefore;

    public HasInjectedAttributesInterceptorFactory(boolean mutateEventBefore) {
      this.mutateEventBefore = mutateEventBefore;
    }

    @Override
    public ProcessorInterceptor get() {
      return new HasInjectedAttributesInterceptor(expressionEvaluator, lockFactory, httpService, errorTypeRepository,
                                                  schedulerService, registry, mutateEventBefore);
    }
  }

  public static class HasInjectedAttributesInterceptor implements ProcessorInterceptor {

    static final List<InterceptionParameters> interceptionParameters = new LinkedList<>();

    private MuleExpressionLanguage expressionEvaluator;
    private LockFactory lockFactory;
    private HttpService httpService;
    private ErrorTypeRepository errorTypeRepository;
    private SchedulerService schedulerService;
    private Registry registry;

    private boolean mutateEventBefore;

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
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event) {
      parameters.values().forEach(ProcessorParameterValue::resolveValue);
      interceptionParameters.add(new InterceptionParameters(location, parameters, event));
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

    private ComponentLocation location;
    private Map<String, ProcessorParameterValue> parameters;
    private InterceptionEvent event;

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
  }

  public static class EvaluatesExpressionInterceptorFactory implements ProcessorInterceptorFactory {

    @Inject
    private MuleExpressionLanguage expressionEvaluator;

    public EvaluatesExpressionInterceptorFactory() {}

    @Override
    public ProcessorInterceptor get() {
      return new EvaluatesExpressionInterceptor(expressionEvaluator);
    }

    @Override
    public boolean intercept(ComponentLocation location) {
      return location.getLocation().startsWith("expressionsInInterception");
    }
  }

  public static class EvaluatesExpressionInterceptor implements ProcessorInterceptor {

    private MuleExpressionLanguage expressionEvaluator;

    public EvaluatesExpressionInterceptor(MuleExpressionLanguage expressionEvaluator) {
      this.expressionEvaluator = expressionEvaluator;
    }

    @Override
    public void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters, InterceptionEvent event) {
      parameters.values().forEach(ProcessorParameterValue::resolveValue);
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

  public static class AfterWithCallbackInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new AfterWithCallbackInterceptor();
    }
  }

  public static class AfterWithCallbackInterceptor implements ProcessorInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> callback;

    @Override
    public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      callback.accept(event, thrown);
    }
  }

}
