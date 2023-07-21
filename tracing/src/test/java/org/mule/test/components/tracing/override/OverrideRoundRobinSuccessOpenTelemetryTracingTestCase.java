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
import org.testcontainers.shaded.org.apache.commons.lang3.function.TriFunction;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class OverrideRoundRobinSuccessOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;
  private static final String CONFIG_FILE = "tracing/round-robin-success.xml";
  private static final String OVERRIDE_FOLDER_NAME = "override/round-robin";
  private static final String FLOW_NAME = "round-robin-flow";
  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  private static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String FLOW_LOCATION = "round-robin-flow";
  private static final String ROUND_ROBIN_LOCATION = "round-robin-flow/processors/0";
  private static final String LOGGER_ROUTE_0_LOCATION = "round-robin-flow/processors/0/route/0/processors/0";
  private static final String LOGGER_ROUTE_1_LOCATION = "round-robin-flow/processors/0/route/1/processors/0";
  private static final String SET_PAYLOAD_LOCATION = "round-robin-flow/processors/0/route/0/processors/1";
  private static final String TEST_ARTIFACT_ID = "OverrideRoundRobinSuccessOpenTelemetryTracingTestCase#testFlow";
  private final String tracingLevelConf;
  private final int expectedSpans;
  private final TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> spanHierarchyRetriever;

  @Inject
  PrivilegedProfilingService profilingService;

  @Parameterized.Parameters(name = "TracingLevelConf: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"overview-round-robin-debug", 5, getOverviewRoundRobinDebugExpectedSpanTestHierarchy()},
        {"overview-round-robin-monitoring", 5, getOverviewRoundRobinMonitoringExpectedSpanTestHierarchy()},
        {"monitoring-round-robin-debug", 5, getMonitoringRoundRobinDebugExpectedSpanTestHierarchy()},
        {"monitoring-round-robin-overview", 1, getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy()},
        {"debug-round-robin-monitoring", 5, getDebugRoundRobinMonitoringExpectedSpanTestHierarchy()},
        {"debug-round-robin-overview", 1, getDebugRoundRobinOverviewExpectedSpanTestHierarchy()}
    });
  }

  public OverrideRoundRobinSuccessOpenTelemetryTracingTestCase(String tracingLevelConf, int expectedSpans,
                                                               TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> spanHierarchyRetriever) {
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

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getOverviewRoundRobinDebugExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId, verifySetPayloadInRoute) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      Map<String, String> loggerSpanAttributeMap;

      if (verifySetPayloadInRoute) {
        loggerSpanAttributeMap = createAttributeMap(LOGGER_ROUTE_0_LOCATION, artifactId);
      } else {
        loggerSpanAttributeMap = createAttributeMap(LOGGER_ROUTE_1_LOCATION, artifactId);
      }

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(ROUND_ROBIN_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(loggerSpanAttributeMap)
          .addAttributesToAssertExistence(attributesToAssertExistence);
      if (verifySetPayloadInRoute) {
        expectedSpanHierarchy
            .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
            .addAttributesToAssertValue(createAttributeMap(SET_PAYLOAD_LOCATION, artifactId))
            .addAttributesToAssertExistence(attributesToAssertExistence);
      }
      expectedSpanHierarchy
          .endChildren()
          .endChildren()
          .endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getOverviewRoundRobinMonitoringExpectedSpanTestHierarchy() {
    return getOverviewRoundRobinDebugExpectedSpanTestHierarchy();
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getMonitoringRoundRobinDebugExpectedSpanTestHierarchy() {
    return getOverviewRoundRobinDebugExpectedSpanTestHierarchy();
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, artifactId, verifySetPayloadInRoute) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, artifactId))
          .addAttributesToAssertExistence(attributesToAssertExistence);

      return expectedSpanHierarchy;
    };
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getDebugRoundRobinMonitoringExpectedSpanTestHierarchy() {
    return getOverviewRoundRobinDebugExpectedSpanTestHierarchy();
  }

  private static TriFunction<Collection<CapturedExportedSpan>, String, Boolean, SpanTestHierarchy> getDebugRoundRobinOverviewExpectedSpanTestHierarchy() {
    return getMonitoringRoundRobinOverviewExpectedSpanTestHierarchy();
  }

  @Test
  public void testFlow() throws Exception {
    // We send three requests to verify that the traces correspond to the round-robin operation.
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), expectedSpans, true);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(),
                         tracingLevelConf.endsWith("round-robin-overview") ? 1 : expectedSpans - 1, false);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), expectedSpans, true);
  }

  private void assertRoundRobinSpan(ExportedSpanSniffer spanCapturer, int numberOfExpectedSpans, boolean verifySetPayloadInRoute)
      throws Exception {
    try {
      flowRunner(FLOW_NAME).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == numberOfExpectedSpans;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured.";
        }
      });


      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      spanHierarchyRetriever.apply(exportedSpans, TEST_ARTIFACT_ID + "[TracingLevelConf: " + tracingLevelConf + "]",
                                   verifySetPayloadInRoute)
          .assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
