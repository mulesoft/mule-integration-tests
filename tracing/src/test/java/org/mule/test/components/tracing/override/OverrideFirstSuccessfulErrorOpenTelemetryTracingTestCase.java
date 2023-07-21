/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing.override;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.util.Arrays.asList;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.components.tracing.OpenTelemetryTracingTestRunnerConfigAnnotation;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OverrideFirstSuccessfulErrorOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String CONFIG_FILE = "tracing/first-successful-error.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/first-successful";
  private static final String FLOW_NAME = "first-successful-telemetryFlow";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_FIRST_SUCCESSFUL_SPAN_NAME = "mule:first-successful";
  private static final String EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_1 = "mule:first-successful:attempt:1";
  private static final String EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_2 = "mule:first-successful:attempt:2";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  private static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  private static final String ANY_EXPECTED_ERROR_TYPE = "ANY:EXPECTED";
  private final String tracingLevelConf;
  private final int expectedSpansCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-first-successful-debug", 8, getOverviewFirstSuccessfulDebugExpectedSpanTestHierarchy()},
        {"overview-first-successful-monitoring", 8, getOverviewFirstSuccessfulMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-first-successful-debug", 10, getMonitoringFirstSuccessfulDebugExpectedSpanTestHierarchy()},
        {"monitoring-first-successful-overview", 3, getMonitoringFirstSuccessfulOverviewExpectedSpanTestHierarchy()},
        {"debug-first-successful-monitoring", 10, getDebugFirstSuccessfulMonitoringExpectedSpanTestHierarchy()},
        {"debug-first-successful-overview", 3, getDebugFirstSuccessfulOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideFirstSuccessfulErrorOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpansCount,
                                                                  Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.tracingLevelConf = tracingLevelConf;
    this.expectedSpansCount = expectedSpansCount;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @Override
  protected String getConfigFile() {
    return CONFIG_FILE;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    String separator = getDefault().getSeparator();
    String tracingLevelConfigurationPath = format("%s%s%s%s", OVERRIDE_FOLDER_NAME, separator, tracingLevelConf, separator);
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, tracingLevelConfigurationPath);
    super.doSetUpBeforeMuleContextCreation();
  }

  @After
  public void doAfter() {
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewFirstSuccessfulDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_FIRST_SUCCESSFUL_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_1).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_2)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewFirstSuccessfulMonitoringExpectedSpanTestHierarchy() {
    return getOverviewFirstSuccessfulDebugExpectedSpanTestHierarchy();
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringFirstSuccessfulDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_FIRST_SUCCESSFUL_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_1).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME_ATTEMPT_2)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringFirstSuccessfulOverviewExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugFirstSuccessfulMonitoringExpectedSpanTestHierarchy() {
    return getMonitoringFirstSuccessfulDebugExpectedSpanTestHierarchy();
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugFirstSuccessfulOverviewExpectedSpanTestHierarchy() {
    return getMonitoringFirstSuccessfulOverviewExpectedSpanTestHierarchy();
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(FLOW_NAME).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).dispatch();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == expectedSpansCount;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured.";
        }
      });


      spanHierarchyRetriever.apply(spanCapturer.getExportedSpans()).assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }
}
