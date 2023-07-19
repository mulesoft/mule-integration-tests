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
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.ARTIFACT_ID_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

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
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;
import org.testcontainers.shaded.org.apache.commons.lang3.function.TriFunction;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class RoundRobinSuccessfulOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String ROUND_ROBIN_FLOW = "round-robin-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  public static final String TEST_ARTIFACT_ID = "RoundRobinSuccessfulOpenTelemetryTracingTestCase#testRoundRobinFlow";
  private final String traceLevel;
  private final int expectedSpans;
  private final TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/round-robin-success.xml";
  }

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 5, getMonitoringExpectedSpanTestHierarchy()},
        {DEBUG.name(), 5, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId, verifySetPayloadInRoute) -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME);
      return expectedSpanHierarchy;
    };
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId, verifySetPayloadInRoute) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      Map<String, String> loggerSpanAttributeMap;
      if (verifySetPayloadInRoute) {
        loggerSpanAttributeMap = createAttributeMap("round-robin-flow/processors/0/route/0/processors/0", artifactId);
      } else {
        loggerSpanAttributeMap = createAttributeMap("round-robin-flow/processors/0/route/1/processors/0", artifactId);
      }

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(loggerSpanAttributeMap)
          .addAttributesToAssertExistence(attributesToAssertExistence);
      if (verifySetPayloadInRoute) {
        expectedSpanHierarchy
            .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
            .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0/route/0/processors/1",
                                                           artifactId))
            .addAttributesToAssertExistence(attributesToAssertExistence);
      }
      expectedSpanHierarchy
          .endChildren()
          .endChildren()
          .endChildren();

      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));

      return expectedSpanHierarchy;
    };
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
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

  public RoundRobinSuccessfulOpenTelemetryTracingTestCase(String traceLevel, int expectedSpans,
                                                          TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> spanHierarchyRetriever) {
    this.traceLevel = traceLevel;
    this.expectedSpans = expectedSpans;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @Test
  public void testRoundRobinFlow() throws Exception {
    // We send three requests to verify that the tracing to verify the round robin functioning
    // and that the traces corresponds to that.
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), expectedSpans, true);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(),
                         traceLevel.equals("OVERVIEW") ? 1 : expectedSpans - 1, false);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), expectedSpans, true);
  }

  private void assertRoundRobinSpan(ExportedSpanSniffer spanCapturer, int numberOfExpectedSpans, boolean verifySetPayloadInRoute)
      throws Exception {
    try {
      flowRunner(ROUND_ROBIN_FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == numberOfExpectedSpans;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      spanHierarchyRetriever.apply(spanCapturer.getExportedSpans(), TEST_ARTIFACT_ID + "[tracingLevel: " + traceLevel + "]",
                                   verifySetPayloadInRoute)
          .assertSpanTree();

    } finally {
      spanCapturer.dispose();
    }
  }
}

