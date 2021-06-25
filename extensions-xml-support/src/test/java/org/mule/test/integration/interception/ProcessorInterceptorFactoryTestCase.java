/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.interception;

import static java.lang.Math.random;
import static java.util.Arrays.asList;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mule.functional.api.exception.ExpectedError.none;
import static org.mule.runtime.api.interception.ProcessorInterceptorFactory.INTERCEPTORS_ORDER_REGISTRY_KEY;
import static org.mule.runtime.extension.api.ExtensionConstants.ERROR_MAPPINGS_PARAMETER_NAME;
import static org.mule.tck.probe.PollingProber.check;
import static org.mule.test.allure.AllureConstants.InterceptonApi.INTERCEPTION_API;
import static org.mule.test.allure.AllureConstants.InterceptonApi.ComponentInterceptionStory.COMPONENT_INTERCEPTION_STORY;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.functional.api.exception.ExpectedError;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.ComponentIdentifier;
import org.mule.runtime.api.component.location.ComponentLocation;
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
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import javax.inject.Inject;

import org.junit.After;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(XML_SDK), @Feature(INTERCEPTION_API)})
@Story(COMPONENT_INTERCEPTION_STORY)
public class ProcessorInterceptorFactoryTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  private static final int POLLING_TIMEOUT = 5000;
  private static final int POLLING_DELAY = 500;

  private AfterWithCallbackInterceptorFactory afterWithCallbackInterceptorFactory;
  private List<ProcessorInterceptor> processorInterceptors;

  @Rule
  public ExpectedError expectedError = none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/interception/processor-interceptor-factory.xml";
  }

  @Override
  protected Map<String, Object> getStartUpRegistryObjects() {
    Map<String, Object> objects = new HashMap<>();
    processorInterceptors = new LinkedList<>();
    afterWithCallbackInterceptorFactory = new AfterWithCallbackInterceptorFactory(processorInterceptors);
    objects.put("_AfterWithCallbackInterceptorFactory", afterWithCallbackInterceptorFactory);
    objects.put("_HasInjectedAttributesInterceptorFactory", new HasInjectedAttributesInterceptorFactory(false));
    objects.put("_EvaluatesExpressionInterceptorFactory", new EvaluatesExpressionInterceptorFactory());

    objects.put(INTERCEPTORS_ORDER_REGISTRY_KEY,
                (ProcessorInterceptorOrder) () -> asList(AfterWithCallbackInterceptorFactory.class.getName(),
                                                         HasInjectedAttributesInterceptorFactory.class.getName(),
                                                         EvaluatesExpressionInterceptorFactory.class.getName()));

    return objects;
  }

  @After
  public void after() {
    HasInjectedAttributesInterceptor.interceptionParameters.clear();
    AfterWithCallbackInterceptor.callback = (event, thrown) -> {
    };
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
               containsInAnyOrder("doc:name", "targetValue", ERROR_MAPPINGS_PARAMETER_NAME));
    assertThat(moduleOperationChain.getParameters().get("doc:name").resolveValue(), is("mySCName"));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is("Wubba Lubba Dub Dub"));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
  }

  @Test
  public void interceptionClassLoader() throws Exception {
    flowRunner("scOperation").run();

    assertThat(processorInterceptors, is(not(empty())));
    processorInterceptors.stream().map(i -> (AfterWithCallbackInterceptor) i).forEach(processorInterceptor -> {
      assertThat(processorInterceptor.getAfterClassLoader(), is(processorInterceptor.getClassLoader()));
    });
  }

  @Description("Smart Connector inside a sub-flow declares a simple operation without parameters")
  @Test
  public void scOperationInsideSubFlow() throws Exception {
    flowRunner("scOperationInsideSubFlow").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier flowRefIdentifier = interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleOperationChainIdentifier =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(flowRefIdentifier.getName(), equalTo("flow-ref"));

    assertThat(moduleOperationChainIdentifier.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleOperationChainIdentifier.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a choice router declares a simple operation without parameters")
  @Test
  public void scOperationInsideChoiceRouter() throws Exception {
    flowRunner("flowWithChoiceRouter").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier choiceIdentifier = interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleName =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(choiceIdentifier.getName(), equalTo("choice"));

    assertThat(moduleName.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleName.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a until-successful scope declares a simple operation without parameters")
  @Issue("MULE-16285")
  @Test
  public void scOperationInsideAnUntilSuccessScope() throws Exception {
    flowRunner("flowWithUntilSuccessfulScope").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier untilSuccessfulIdentifier =
        interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleName =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(untilSuccessfulIdentifier.getName(), equalTo("until-successful"));

    assertThat(moduleName.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleName.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a try scope declares a simple operation without parameters")
  @Issue("MULE-16285")
  @Test
  public void scOperationInsideTryScope() throws Exception {
    flowRunner("flowWithTryScope").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier tryIdentifier =
        interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleName =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(tryIdentifier.getName(), equalTo("try"));

    assertThat(moduleName.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleName.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a foreach scope declares a simple operation without parameters")
  @Issue("MULE-16285")
  @Test
  public void scOperationInsideForeachScope() throws Exception {
    flowRunner("flowWithForeachScope").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier foreachIdentifier =
        interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleName =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(foreachIdentifier.getName(), equalTo("foreach"));

    assertThat(moduleName.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleName.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a parallel-foreach scope declares a simple operation without parameters")
  @Issue("MULE-16285")
  @Test
  public void scOperationInsideParallelForeachScope() throws Exception {
    flowRunner("flowWithParallelForeachScope").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(3));

    ComponentIdentifier parallelForeachIdentifier =
        interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier moduleName =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();
    ComponentIdentifier setPayloadOperationIdentifier =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(parallelForeachIdentifier.getName(), equalTo("parallel-foreach"));

    assertThat(moduleName.getNamespace(), equalTo("module-using-core"));
    assertThat(moduleName.getName(), equalTo("set-payload-hardcoded"));

    assertThat(setPayloadOperationIdentifier.getName(), equalTo("set-payload"));
  }

  @Description("Smart Connector inside a scatter-gather declares a simple operation without parameters")
  @Issue("MULE-16285")
  @Test
  public void flowWithScatterGather() throws Exception {
    flowRunner("flowWithScatterGather").run();

    List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
    assertThat(interceptionParameters, hasSize(4));

    ComponentIdentifier scatterGatherIdentifier =
        interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();

    ComponentIdentifier firstRoute =
        interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();

    ComponentIdentifier thirdInterceptorParameter =
        interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

    ComponentIdentifier fourthInterceptorParameter =
        interceptionParameters.get(3).getLocation().getComponentIdentifier().getIdentifier();

    assertThat(scatterGatherIdentifier.getName(), equalTo("scatter-gather"));

    if (firstRoute.getName().equals("set-payload-hardcoded")) {
      assertThat(asList(thirdInterceptorParameter.getName(), fourthInterceptorParameter.getName()),
                 hasItems("logger", "set-payload"));
      assertThat(firstRoute.getNamespace(), equalTo("module-using-core"));
    } else {
      assertThat(asList(thirdInterceptorParameter.getName(), fourthInterceptorParameter.getName()),
                 hasItems("set-payload-hardcoded", "set-payload"));

      assertThat(firstRoute.getNamespace(), equalTo("mule"));
      assertThat(firstRoute.getName(), equalTo("logger"));
    }
  }

  @Description("Smart connectors inside async are not skipped properly")
  @Issue("MULE-17020")
  @Test
  public void flowWithAsyncScope() throws Exception {
    flowRunner("flowWithAsyncScope").run();

    check(POLLING_TIMEOUT, POLLING_DELAY, () -> {
      List<InterceptionParameters> interceptionParameters = HasInjectedAttributesInterceptor.interceptionParameters;
      assertThat(interceptionParameters, hasSize(3));

      ComponentIdentifier asyncIdentifier =
          interceptionParameters.get(0).getLocation().getComponentIdentifier().getIdentifier();

      ComponentIdentifier smartConnectorIdentifier =
          interceptionParameters.get(1).getLocation().getComponentIdentifier().getIdentifier();

      ComponentIdentifier setPayloadIdentifier =
          interceptionParameters.get(2).getLocation().getComponentIdentifier().getIdentifier();

      assertThat(asyncIdentifier.getName(), equalTo("async"));

      assertThat(smartConnectorIdentifier.getNamespace(), equalTo("module-using-core"));
      assertThat(smartConnectorIdentifier.getName(), equalTo("set-payload-hardcoded"));

      assertThat(setPayloadIdentifier.getNamespace(), equalTo("mule"));
      assertThat(setPayloadIdentifier.getName(), equalTo("set-payload"));

      return true;
    });

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

    assertThat(moduleOperationChain.getParameters().keySet(),
               containsInAnyOrder("echoMessage", "targetValue", ERROR_MAPPINGS_PARAMETER_NAME));
    assertThat(moduleOperationChain.getParameters().get("echoMessage").resolveValue(), is("echo message for the win"));

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

    assertThat(moduleOperationChain.getParameters().keySet(),
               containsInAnyOrder("echoMessage", "targetValue", ERROR_MAPPINGS_PARAMETER_NAME));
    assertThat(moduleOperationChain.getParameters().get("echoMessage").resolveValue(), is("echo message for the win"));

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

    assertThat(proxyModuleOperationChain.getParameters().keySet(),
               containsInAnyOrder("targetValue", ERROR_MAPPINGS_PARAMETER_NAME));
    assertThat(innerModuleOperationChain.getParameters().keySet(),
               containsInAnyOrder("targetValue", ERROR_MAPPINGS_PARAMETER_NAME));

    assertThat(setPayloadOperation.getParameters().keySet(), containsInAnyOrder("value", "mimeType", "encoding"));
    assertThat(setPayloadOperation.getParameters().get("value").resolveValue(), is("Wubba Lubba Dub Dub"));
    assertThat(setPayloadOperation.getParameters().get("mimeType").resolveValue(), is("text/plain"));
    assertThat(setPayloadOperation.getParameters().get("encoding").resolveValue(), is("UTF-8"));
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

    private final boolean mutateEventBefore;

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
    public synchronized void before(ComponentLocation location, Map<String, ProcessorParameterValue> parameters,
                                    InterceptionEvent event) {
      parameters.values().forEach(v -> {
        try {
          v.resolveValue();
        } catch (ExpressionRuntimeException e) {
          // Ignore so that the evaluation continues
        }
      });
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

    private final MuleExpressionLanguage expressionEvaluator;

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

    List<ProcessorInterceptor> processorInterceptors;

    public AfterWithCallbackInterceptorFactory(List<ProcessorInterceptor> processorInterceptors) {
      this.processorInterceptors = processorInterceptors;
    }

    @Override
    public ProcessorInterceptor get() {
      return new AfterWithCallbackInterceptor(processorInterceptors);
    }
  }

  public static class AfterWithCallbackInterceptor implements ProcessorInterceptor {

    static BiConsumer<InterceptionEvent, Optional<Throwable>> callback = (event, thrown) -> {
    };

    private ClassLoader afterClassLoader;

    List<ProcessorInterceptor> processorInterceptors;

    public AfterWithCallbackInterceptor(List<ProcessorInterceptor> processorInterceptors) {
      this.processorInterceptors = processorInterceptors;
    }

    @Override
    public void after(ComponentLocation location, InterceptionEvent event, Optional<Throwable> thrown) {
      processorInterceptors.add(this);
      this.afterClassLoader = Thread.currentThread().getContextClassLoader();
      callback.accept(event, thrown);
    }

    public ClassLoader getAfterClassLoader() {
      return afterClassLoader;
    }

    public ClassLoader getClassLoader() {
      return this.getClass().getClassLoader();
    }
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
