/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
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

import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.function.Function;

import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class PagingConnectionTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String FLOW = "flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String HEISENBERG_GET_PAGED_BLOCKLIST_SPAN_NAME = "heisenberg:get-paged-blocklist";
  public static final String MULE_OPERATION_EXECUTION_SPAN_NAME = "mule:operation-execution";
  public static final String MULE_PARAMETERS_RESOLUTION_SPAN_NAME = "mule:parameters-resolution";
  public static final String MULE_VALUE_RESOLUTION_SPAN_NAME = "mule:value-resolution";
  public static final String MULE_GET_CONNECTION_SPAN_NAME = "mule:get-connection";
  public static final String MULE_PARALLEL_FOREACH_SPAN_NAME = "mule:parallel-foreach";
  public static final String MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME = "mule:parallel-foreach:iteration";
  public static final String MULE_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private final String tracingLevel;
  private final int expectedSpans;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/paging-connection-tracing.xml";
  }

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {MONITORING.name(), 15, getMonitoringExpectedSpanTestHierarchy()},
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {DEBUG.name(), 25, getDebugExpectedSpanTestHierarchy()}
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
          .child(HEISENBERG_GET_PAGED_BLOCKLIST_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_SPAN_NAME)
          .beginChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
    return exportedSpans -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(HEISENBERG_GET_PAGED_BLOCKLIST_SPAN_NAME)
          .beginChildren()
          .child(MULE_OPERATION_EXECUTION_SPAN_NAME)
          .child(MULE_PARAMETERS_RESOLUTION_SPAN_NAME)
          .beginChildren()
          .child(MULE_VALUE_RESOLUTION_SPAN_NAME)
          .child(MULE_VALUE_RESOLUTION_SPAN_NAME)
          .child(MULE_VALUE_RESOLUTION_SPAN_NAME)
          .child(MULE_VALUE_RESOLUTION_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_SPAN_NAME)
          .beginChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .child(MULE_PARALLEL_FOREACH_ITERATION_SPAN_NAME)
          .beginChildren()
          .child(MULE_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  public PagingConnectionTracingTestCase(String tracingLevel, int expectedSpans,
                                         Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.tracingLevel = tracingLevel;
    this.expectedSpans = expectedSpans;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, tracingLevel.toLowerCase() + FileSystems.getDefault().getSeparator());
    super.doSetUpBeforeMuleContextCreation();
  }


  @After
  public void doAfter() {
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
  }

  @Test
  public void test() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run();
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == expectedSpans;
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
