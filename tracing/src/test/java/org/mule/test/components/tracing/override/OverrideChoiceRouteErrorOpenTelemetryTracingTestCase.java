/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.components.tracing.OpenTelemetryTracingSnifferTestCase;
import org.mule.test.components.tracing.OpenTelemetryTracingTestRunnerConfigAnnotation;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OverrideChoiceRouteErrorOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String CONFIG_FILE = "tracing/choice-router.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/choice-route";
  private static final String FLOW_NAME = "choice-flow";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_CHOICE_SPAN_NAME = "mule:choice";
  private static final String EXPECTED_ROUTE_SPAN_NAME = "mule:choice:route";
  private static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  private static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  private static final String ANY_EXPECTED_ERROR_TYPE = "ANY:EXPECTED";
  private static final String UNKNOWN_LOCATION = "unknown";
  private static final String CHOICE_FLOW_LOCATION = "choice-flow";
  private static final String CHOICE_ROUTE_LOCATION = "choice-flow/processors/0";
  private static final String RAISE_ERROR_LOCATION = "choice-flow/processors/0/route/1/processors/0";
  private static final String TEST_ARTIFACT_ID = "OverrideChoiceRouteErrorOpenTelemetryTracingTestCase#testFlow";
  private final String tracingLevelConf;
  private final int expectedSpansCount;
  private final BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-choice-route-debug", 4, getOverviewChoiceRouteDebugExpectedSpanTestHierarchy()},
        {"overview-choice-route-monitoring", 4, getOverviewChoiceRouteMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-choice-route-debug", 5, getMonitoringChoiceRouteDebugExpectedSpanTestHierarchy()},
        {"monitoring-choice-route-overview", 2, getMonitoringChoiceRouteOverviewExpectedSpanTestHierarchy()},
        {"debug-choice-route-monitoring", 5, getDebugChoiceRouteMonitoringExpectedSpanTestHierarchy()},
        {"debug-choice-route-overview", 2, getDebugChoiceRouteOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideChoiceRouteErrorOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpansCount,
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

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewChoiceRouteDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_CHOICE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_ROUTE_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_ROUTE_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(RAISE_ERROR_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewChoiceRouteMonitoringExpectedSpanTestHierarchy() {
    return getOverviewChoiceRouteDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringChoiceRouteDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_CHOICE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_ROUTE_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_ROUTE_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(RAISE_ERROR_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(UNKNOWN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringChoiceRouteOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ANY_EXPECTED_ERROR_TYPE)
          .addAttributesToAssertValue(createAttributeMap(CHOICE_FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(UNKNOWN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugChoiceRouteMonitoringExpectedSpanTestHierarchy() {
    return getMonitoringChoiceRouteDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugChoiceRouteOverviewExpectedSpanTestHierarchy() {
    return getMonitoringChoiceRouteOverviewExpectedSpanTestHierarchy();
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(FLOW_NAME).withPayload(EXPECTED_RAISE_ERROR_SPAN_NAME).runExpectingException();

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


      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      String artifactId = TEST_ARTIFACT_ID + "[TracingLevelConf: " + tracingLevelConf + "]";
      spanHierarchyRetriever.apply(exportedSpans, artifactId).assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
