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
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.ARTIFACT_ID_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static java.lang.String.format;
import static java.lang.System.clearProperty;
import static java.lang.System.setProperty;
import static java.nio.file.FileSystems.getDefault;
import static java.util.Arrays.asList;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

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
import java.util.function.BiFunction;

import jakarta.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OverrideRoundRobinErrorOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String CONFIG_FILE = "tracing/round-robin-error.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/round-robin";
  private static final String FLOW_NAME = "round-robin-flow";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  private static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  private static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  private static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN = "mule:on-error-propagate";
  private static final String ANY_EXPECTED_ERROR_TYPE = "ANY:EXPECTED";
  private static final String FLOW_LOCATION = "round-robin-flow";
  private static final String ROUND_ROBIN_LOCATION = "round-robin-flow/processors/0";
  private static final String LOGGER_LOCATION = "round-robin-flow/processors/0/route/0/processors/0";
  private static final String RAISE_ERROR_LOCATION = "round-robin-flow/processors/0/route/0/processors/1";
  private static final String UNKNOWN_LOCATION = "unknown";
  private static final String TEST_ARTIFACT_ID = "OverrideRoundRobinErrorOpenTelemetryTracingTestCase#testFlow";
  private final String tracingLevelConf;
  private final int expectedSpansCount;
  private final BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-round-robin-debug", 5, getOverviewRoundRobinDebugExpectedSpanTestHierarchy()},
        {"overview-round-robin-monitoring", 5, getOverviewRoundRobinMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-round-robin-debug", 6, getMonitoringRoundRobinDebugExpectedSpanTestHierarchy()},
        {"monitoring-round-robin-overview", 2, getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy()},
        {"debug-round-robin-monitoring", 6, getDebugRoundRobinMonitoringExpectedSpanTestHierarchy()},
        {"debug-round-robin-overview", 2, getDebugRoundRobinOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideRoundRobinErrorOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpansCount,
                                                             BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever) {
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

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewRoundRobinDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(LOGGER_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(RAISE_ERROR_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewRoundRobinMonitoringExpectedSpanTestHierarchy() {
    return getOverviewRoundRobinDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringRoundRobinDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(LOGGER_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(RAISE_ERROR_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN)
          .addAttributesToAssertValue(createAttributeMap(UNKNOWN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN)
          .addAttributesToAssertValue(createAttributeMap(UNKNOWN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugRoundRobinMonitoringExpectedSpanTestHierarchy() {
    return getMonitoringRoundRobinDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugRoundRobinOverviewExpectedSpanTestHierarchy() {
    return getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy();
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(FLOW_NAME).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).runExpectingException();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == expectedSpansCount;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured.";
        }
      });


      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      spanHierarchyRetriever.apply(exportedSpans, TEST_ARTIFACT_ID + "[TracingLevelConf: " + tracingLevelConf + "]")
          .assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
