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
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.ARTIFACT_ID_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

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
import java.util.List;
import java.util.function.BiFunction;

import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

import jakarta.inject.Inject;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class RoundRobinErrorOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String ROUND_ROBIN_FLOW = "round-robin-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  public static final String TEST_ARTIFACT_ID = "RoundRobinErrorOpenTelemetryTracingTestCase#testRoundRobinFlowWithError";
  private final String traceLevel;
  private final int expectedSpansCount;
  private final BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 6, getMonitoringExpectedSpanTestHierarchy()},
        {DEBUG.name(), 6, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("ANY:EXPECTED");
      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData("ANY:EXPECTED")
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME).addExceptionData("ANY:EXPECTED")
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME).addExceptionData("ANY:EXPECTED")
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0/route/0/processors/0", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData("ANY:EXPECTED")
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0/route/0/processors/1", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN)
          .addAttributesToAssertValue(createAttributeMap("unknown", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
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

  public RoundRobinErrorOpenTelemetryTracingTestCase(String traceLevel, int expectedSpansCount,
                                                     BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever) {
    this.traceLevel = traceLevel;
    this.expectedSpansCount = expectedSpansCount;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }


  @Override
  protected String getConfigFile() {
    return "tracing/round-robin-error.xml";
  }

  @Test
  public void testRoundRobinFlowWithError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(ROUND_ROBIN_FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).runExpectingException();

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

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      spanHierarchyRetriever.apply(exportedSpans, TEST_ARTIFACT_ID + "[tracingLevel: " + traceLevel + "]").assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
