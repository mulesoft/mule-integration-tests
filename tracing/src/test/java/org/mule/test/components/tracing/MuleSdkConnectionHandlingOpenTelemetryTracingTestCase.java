/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

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

import static java.lang.String.format;
import static java.util.Arrays.asList;

import org.mule.runtime.api.config.custom.ServiceConfigurator;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.customization.api.InternalSpanNames;
import org.mule.runtime.tracing.level.api.config.TracingLevel;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import javax.inject.Inject;
import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;

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
  public static final String EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN = "petstore:get-connection-age";
  public static final String EXPECTED_PETSTORE_GET_PETS_SPAN = "petstore:get-pets";
  public static final String EXPECTED_MULE_GET_CONNECTION_SPAN = "mule:get-connection";
  private static final String OPERATION_WITH_SIMPLE_CONNECTION = "operation-with-simple-connection";

  private static final String OPERATION_WITH_SIMPLE_FAILING_CONNECTION = "operation-with-simple-failing-connection";
  private ExportedSpanSniffer spanCapturer;
  @Inject
  private PrivilegedProfilingService profilingService;
  private final String lazyConnections;
  private final TracingLevel tracingLevel;

  @Rule
  public SystemProperty tracingLevelConfigFilePath;

  public MuleSdkConnectionHandlingOpenTelemetryTracingTestCase(String lazyConnections, TracingLevel tracingLevel) {
    this.lazyConnections = lazyConnections;
    this.tracingLevel = tracingLevel;
    tracingLevelConfigFilePath = new SystemProperty(TRACING_LEVEL_CONFIGURATION_PATH,
                                                    tracingLevel.name().toLowerCase() + FileSystems.getDefault().getSeparator());

  }

  @Override
  protected String getConfigFile() {
    return "tracing/mule-sdk-connection-handling.xml";
  }

  @Parameterized.Parameters(name = "lazyConnections: {0} - tracingLevel: {1}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"true", OVERVIEW},
        {"false", OVERVIEW},
        {"true", MONITORING},
        {"false", MONITORING},
        {"true", DEBUG},
        {"false", DEBUG}
    });
  }

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
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
  public void testOperationWithSimpleConnection() throws Exception {

    flowRunner(OPERATION_WITH_SIMPLE_CONNECTION).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();
    assertSpans();
  }

  @Test
  public void testOperationWithFailingSimpleConnection() throws Exception {
    flowRunner(OPERATION_WITH_SIMPLE_FAILING_CONNECTION).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(ErrorTypeMatcher.errorType("PETSTORE", "CONNECTIVITY"));
    assertFailingSpans();
  }

  private void assertSpans() {
    if (tracingLevel.equals(OVERVIEW)) {
      waitForSpans(1);
      assertOverviewSpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(MONITORING)) {
      waitForSpans(2);
      assertMonitoringSpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(DEBUG) && lazyConnections.equals("true")) {
      waitForSpans(7);
      assertDebugLazySpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(DEBUG) && lazyConnections.equals("false")) {
      waitForSpans(7);
      assertDebugEagerSpans(spanCapturer.getExportedSpans());
    } else {
      throw new IllegalArgumentException(format("Unrecognized tracing level: %s", tracingLevel.name()));
    }
  }

  private void assertOverviewSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME);
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertMonitoringSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN);
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertDebugEagerSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN)
        .beginChildren()
        .child(EXPECTED_MULE_GET_CONNECTION_SPAN)
        .child(PARAMETERS_RESOLUTION_SPAN_NAME)
        .beginChildren()
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .child(VALUE_RESOLUTION_SPAN_NAME)
        .endChildren()
        .child(OPERATION_EXECUTION_SPAN_NAME)
        .endChildren();
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertDebugLazySpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_PETSTORE_GET_CONNECTION_AGE_SPAN)
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
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertFailingSpans() {
    if (tracingLevel.equals(OVERVIEW)) {
      waitForSpans(1);
      assertOverviewFailingSpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(MONITORING)) {
      waitForSpans(3);
      assertMonitoringFailingSpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(DEBUG) && lazyConnections.equals("true")) {
      waitForSpans(10);
      assertDebugLazyFailingSpans(spanCapturer.getExportedSpans());
    } else if (tracingLevel.equals(DEBUG) && lazyConnections.equals("false")) {
      waitForSpans(9);
      assertDebugEagerFailingSpans(spanCapturer.getExportedSpans());
    } else {
      throw new IllegalArgumentException(format("Unrecognized tracing level: %s", tracingLevel.name()));
    }
  }

  private void assertOverviewFailingSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY");
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertMonitoringFailingSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PETSTORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY");
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertDebugLazyFailingSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PETSTORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
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
    expectedSpanHierarchy.assertSpanTree();
  }

  private void assertDebugEagerFailingSpans(Collection<CapturedExportedSpan> capturedExportedSpans) {
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("PETSTORE:CONNECTIVITY")
        .beginChildren()
        .child(InternalSpanNames.ON_ERROR_PROPAGATE_SPAN_NAME)
        .child(EXPECTED_PETSTORE_GET_PETS_SPAN).addExceptionData("PETSTORE:CONNECTIVITY")
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
    expectedSpanHierarchy.assertSpanTree();
  }

  private void waitForSpans(int numberOfSpans) {
    new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS).check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
        return exportedSpans.size() == numberOfSpans;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });
  }
}
