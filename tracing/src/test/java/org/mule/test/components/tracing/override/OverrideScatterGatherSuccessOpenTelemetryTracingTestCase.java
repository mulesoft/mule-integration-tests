/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.components.tracing.OpenTelemetryTracingSnifferTestCase;
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
public class OverrideScatterGatherSuccessOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String FLOW_NAME = "scatter-gather-flow";
  private static final String CONFIG_FILE = "tracing/scatter-gather-success.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/scatter-gather";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SCATTER_GATHER_SPAN_NAME = "mule:scatter-gather";
  private static final String EXPECTED_ROUTE_SPAN_NAME = "mule:scatter-gather:route";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  private final String tracingLevelConf;
  private final int expectedSpans;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-scatter-gather-debug", 6, getOverviewScatterGatherDebugExpectedSpanTestHierarchy()},
        {"overview-scatter-gather-monitoring", 6, getOverviewScatterGatherMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-scatter-gather-debug", 7, getMonitoringScatterGatherDebugExpectedSpanTestHierarchy()},
        {"monitoring-scatter-gather-overview", 2, getMonitoringScatterGatherOverviewExpectedSpanTestHierarchy()},
        {"debug-scatter-gather-monitoring", 7, getDebugScatterGatherMonitoringExpectedSpanTestHierarchy()},
        {"debug-scatter-gather-overview", 2, getDebugScatterGatherOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideScatterGatherSuccessOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpans,
                                                                  Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.tracingLevelConf = tracingLevelConf;
    this.expectedSpans = expectedSpans;
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

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewScatterGatherDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewScatterGatherMonitoringExpectedSpanTestHierarchy() {
    return getOverviewScatterGatherDebugExpectedSpanTestHierarchy();
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringScatterGatherDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringScatterGatherOverviewExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugScatterGatherMonitoringExpectedSpanTestHierarchy() {
    return getMonitoringScatterGatherDebugExpectedSpanTestHierarchy();
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugScatterGatherOverviewExpectedSpanTestHierarchy() {
    return getMonitoringScatterGatherOverviewExpectedSpanTestHierarchy();
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
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == expectedSpans;
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
