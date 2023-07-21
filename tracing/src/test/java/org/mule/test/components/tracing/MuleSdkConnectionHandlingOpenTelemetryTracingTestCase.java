/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.OPERATION_EXECUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.PARAMETERS_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracer.customization.api.InternalSpanNames.VALUE_RESOLUTION_SPAN_NAME;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Arrays.asList;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.customization.api.InternalSpanNames;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import javax.inject.Inject;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.function.Supplier;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.Rule;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class MuleSdkConnectionHandlingOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_PET_STORE_GET_CONNECTION_AGE_SPAN = "petstore:get-connection-age";
  public static final String EXPECTED_PET_STORE_GET_PETS_SPAN = "petstore:get-pets";
  public static final String EXPECTED_MULE_GET_CONNECTION_SPAN = "mule:get-connection";
  private static final String OPERATION_WITH_CONNECTION = "operation-with-connection";
  private static final String OPERATION_WITH_FAILING_CONNECTION = "operation-with-failing-connection";
  private static final String OPERATION_WITH_CONNECTION_RETRY = "operation-with-connection-retry";

  private ExportedSpanSniffer exportedSpanSniffer;

  @Inject
  private PrivilegedProfilingService profilingService;

  private final String lazyConnections;
  private final String flowName;
  private final Supplier<SpanTestHierarchy> expectedTrace;

  @Rule
  public SystemProperty tracingLevelConfigFilePath;

  public MuleSdkConnectionHandlingOpenTelemetryTracingTestCase(String lazyConnections, TracingLevel tracingLevel, String flowName,
                                                               Supplier<SpanTestHierarchy> expectedTrace) {
    this.lazyConnections = lazyConnections;
    this.flowName = flowName;
    this.tracingLevelConfigFilePath = new SystemProperty(TRACING_LEVEL_CONFIGURATION_PATH,
                                                         tracingLevel.name().toLowerCase()
                                                             + FileSystems.getDefault().getSeparator());
    this.expectedTrace = expectedTrace;
  }

  @Override
  protected String getConfigFile() {
    return "tracing/mule-sdk-connection-handling.xml";
  }

  @Parameterized.Parameters(name = "flowName: {2} - tracingLevel: {1} - lazyConnections: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        // Lazy connections, OVERVIEW level
        {"true", OVERVIEW, OPERATION_WITH_CONNECTION, expectedOverviewSpans()},
        {"true", OVERVIEW, OPERATION_WITH_FAILING_CONNECTION, expectedOverviewFailingSpans()},
        {"true", OVERVIEW, OPERATION_WITH_CONNECTION_RETRY, expectedOverviewRetrySpans()},

        // Eager connections, OVERVIEW level
        {"false", OVERVIEW, OPERATION_WITH_CONNECTION, expectedOverviewSpans()},
        {"false", OVERVIEW, OPERATION_WITH_FAILING_CONNECTION, expectedOverviewFailingSpans()},
        {"false", OVERVIEW, OPERATION_WITH_CONNECTION_RETRY, expectedOverviewRetrySpans()},

        // Lazy connections, MONITORING level
        {"true", MONITORING, OPERATION_WITH_CONNECTION, expectedMonitoringSpans()},
        {"true", MONITORING, OPERATION_WITH_FAILING_CONNECTION, expectedMonitoringFailingSpans()},
        {"true", MONITORING, OPERATION_WITH_CONNECTION_RETRY, expectedMonitoringRetrySpans()},

        // Eager connections, MONITORING level
        {"false", MONITORING, OPERATION_WITH_CONNECTION, expectedMonitoringSpans()},
        {"false", MONITORING, OPERATION_WITH_FAILING_CONNECTION, expectedMonitoringFailingSpans()},
        {"false", MONITORING, OPERATION_WITH_CONNECTION_RETRY, expectedMonitoringRetrySpans()},

        // Lazy connections, DEBUG level
        {"true", DEBUG, OPERATION_WITH_CONNECTION, expectedDebugLazySpans()},
        {"true", DEBUG, OPERATION_WITH_FAILING_CONNECTION, expectedDebugLazyFailingSpans()},
        {"true", DEBUG, OPERATION_WITH_CONNECTION_RETRY, expectedDebugLazyRetrySpans()},

        // Eager connections, DEBUG level
        {"false", DEBUG, OPERATION_WITH_CONNECTION, expectedDebugEagerSpans()},
        {"false", DEBUG, OPERATION_WITH_FAILING_CONNECTION, expectedDebugEagerFailingSpans()},
        {"false", DEBUG, OPERATION_WITH_CONNECTION_RETRY, expectedDebugEagerRetrySpans()}
    });
  }

  @Before
  public void initialize() {
    exportedSpanSniffer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void dispose() {
    exportedSpanSniffer.dispose();
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    builders.add(
                 new ConfigurationBuilder() {

                   @Override
                   public void configure(MuleContext muleContext) {
                     muleContext.getDeploymentProperties().setProperty(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY,
                                                                       lazyConnections);
                   }

                   @Override
                   public void addServiceConfigurator(ServiceConfigurator serviceConfigurator) {
                     // Nothing to do
                   }
                 });
  }

  @Test
  public void assertConnectionHandlingTrace() throws Exception {
    try {
      flowRunner(flowName).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
          .run();
    } catch (MuleException e) {
      assertThat(e.getExceptionInfo().getErrorType().getIdentifier(), is("CONNECTIVITY"));
    }
    assertTrace(exportedSpanSniffer, expectedTrace.get());
  }

  private static Supplier<SpanTestHierarchy> expectedOverviewSpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME);
  }

  private static Supplier<SpanTestHierarchy> expectedMonitoringSpans() {
    return () -> new SpanTestHierarchy()
        .withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PET_STORE_GET_CONNECTION_AGE_SPAN)
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugEagerSpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PET_STORE_GET_CONNECTION_AGE_SPAN)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugLazySpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PET_STORE_GET_CONNECTION_AGE_SPAN)
        .beginChildren()
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren()
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedOverviewFailingSpans() {
    return () -> new SpanTestHierarchy()
        .withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY");
  }

  private static Supplier<SpanTestHierarchy> expectedOverviewRetrySpans() {
    // The trace should be the same as the failing one without retries
    return expectedOverviewFailingSpans();
  }

  private static Supplier<SpanTestHierarchy> expectedMonitoringFailingSpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PET_STORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedMonitoringRetrySpans() {
    // The trace should be the same as the failing one without retries
    return expectedMonitoringFailingSpans();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugLazyFailingSpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PET_STORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren()
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugLazyRetrySpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PET_STORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren()
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugEagerFailingSpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PET_STORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren();
  }

  private static Supplier<SpanTestHierarchy> expectedDebugEagerRetrySpans() {
    return () -> new SpanTestHierarchy().withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PET_STORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .endChildren()
        .endChildren();
  }

  private static void assertTrace(ExportedSpanSniffer exportedSpanSniffer, SpanTestHierarchy expectedSpanTestHierarchy) {
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe() {

      private final int expectedSpans = expectedSpanTestHierarchy.size();

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = exportedSpanSniffer.getExportedSpans();
        return exportedSpans.size() == expectedSpans;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });
    expectedSpanTestHierarchy.withCapturedSpans(exportedSpanSniffer.getExportedSpans()).assertSpanTree();
  }
}
