/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevel.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.function.Function;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runners.Parameterized;
import org.junit.After;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class ScatterGatherSuccessOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:scatter-gather:route";
  public static final String EXPECTED_SCATTER_GATHER_SPAN_NAME = "mule:scatter-gather";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String SCATTER_GATHER_FLOW = "scatter-gather-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  private final String traceLevel;
  private final int expectedSpansCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 7, getMonitoringExpectedSpanTestHierarchy()},
        {DEBUG.name(), 7, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME);
      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_SCATTER_GATHER_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .endChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
    // In this case debug and monitoring level are the same.
    return getMonitoringExpectedSpanTestHierarchy();
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, traceLevel.toLowerCase() + FileSystems.getDefault().getSeparator());
    super.doSetUpBeforeMuleContextCreation();
  }

  @After
  public void doAfter() {
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
  }

  public ScatterGatherSuccessOpenTelemetryTracingTestCase(String traceLevel, int expectedSpansCount,
                                                          Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.traceLevel = traceLevel;
    this.expectedSpansCount = expectedSpansCount;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @Override
  protected String getConfigFile() {
    return "tracing/scatter-gather-success.xml";
  }

  @Test
  public void testScatterGatherFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(SCATTER_GATHER_FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == expectedSpansCount;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      spanHierarchyRetriever.apply(spanCapturer.getExportedSpans()).assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }
}
