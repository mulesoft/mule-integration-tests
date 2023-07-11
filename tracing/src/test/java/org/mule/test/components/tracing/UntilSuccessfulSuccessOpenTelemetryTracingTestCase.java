/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.runtime.api.util.MuleSystemProperties.TRACING_LEVEL_CONFIGURATION_PATH;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.DEBUG;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.MONITORING;
import static org.mule.runtime.tracing.level.api.config.TracingLevelId.OVERVIEW;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
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
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class UntilSuccessfulSuccessOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_ATTEMPT_SPAN_NAME = "mule:until-successful:attempt";
  public static final String EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME = "mule:until-successful";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String UNTIL_SUCCESSFUL_FLOW = "until-successful-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  private final String traceLevel;
  private final int expectedSpans;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 5, getMonitoringExpectedSpanTestHierarchy()},
        {DEBUG.name(), 5, getDebugExpectedSpanTestHierarchy()}
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
          .child(EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ATTEMPT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .endChildren()
          .endChildren()
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

  public UntilSuccessfulSuccessOpenTelemetryTracingTestCase(String traceLevel, int expectedSpans,
                                                            Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.traceLevel = traceLevel;
    this.expectedSpans = expectedSpans;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }


  @Override
  protected String getConfigFile() {
    return "tracing/until-successful-success.xml";
  }

  @Test
  public void testUntilSuccessfulFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(UNTIL_SUCCESSFUL_FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == expectedSpans;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      spanHierarchyRetriever.apply(exportedSpans).assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }
}
