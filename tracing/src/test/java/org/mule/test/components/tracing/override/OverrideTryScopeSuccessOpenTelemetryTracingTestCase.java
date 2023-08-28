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
import static org.junit.Assert.assertThat;

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

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OverrideTryScopeSuccessOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String FLOW_NAME = "try-scope-flow";
  private static final String CONFIG_FILE = "tracing/try-scope-successful.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/try-scope";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_TRY_SCOPE_SPAN_NAME = "mule:try";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  private static final String FLOW_LOCATION = "try-scope-flow";
  private static final String TRY_SCOPE_LOCATION = "try-scope-flow/processors/0";
  private static final String LOGGER_LOCATION = "try-scope-flow/processors/0/processors/0";
  private static final String TEST_ARTIFACT_ID = "OverrideTryScopeSuccessOpenTelemetryTracingTestCase#testFlow";
  private final String tracingLevelConf;
  private final int expectedSpansCount;
  private final BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-try-scope-debug", 3, getOverviewTryScopeDebugExpectedSpanTestHierarchy()},
        {"overview-try-scope-monitoring", 3, getOverviewTryScopeMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-try-scope-debug", 3, getMonitoringTryScopeDebugExpectedSpanTestHierarchy()},
        {"monitoring-try-scope-overview", 1, getMonitoringTryScopeOverviewExpectedSpanTestHierarchy()},
        {"debug-try-scope-monitoring", 3, getDebugTryScopeMonitoringExpectedSpanTestHierarchy()},
        {"debug-try-scope-overview", 1, getDebugTryScopeOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideTryScopeSuccessOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpansCount,
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

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewTryScopeDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_TRY_SCOPE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(TRY_SCOPE_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(LOGGER_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getOverviewTryScopeMonitoringExpectedSpanTestHierarchy() {
    return getOverviewTryScopeDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringTryScopeDebugExpectedSpanTestHierarchy() {
    return getOverviewTryScopeDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getMonitoringTryScopeOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence);

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugTryScopeMonitoringExpectedSpanTestHierarchy() {
    return getOverviewTryScopeDebugExpectedSpanTestHierarchy();
  }

  private static BiFunction<Collection<CapturedExportedSpan>, String, SpanTestHierarchy> getDebugTryScopeOverviewExpectedSpanTestHierarchy() {
    return getMonitoringTryScopeOverviewExpectedSpanTestHierarchy();
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(FLOW_NAME).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

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
      spanHierarchyRetriever.apply(exportedSpans, "[TracingLevelConf: " + tracingLevelConf + "]");
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
