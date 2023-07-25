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

import static org.hamcrest.Matchers.hasEntry;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
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
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runners.Parameterized;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class CustomSpanNameAndAttributesTestCaseOpenTelemetry extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  public static final String EXPECTED_SOURCE_SPAN_NAME = "pet-store-list-modified";
  public static final String EXPECTED_CUSTOM_SPAN_NAME = "customSpanName";
  public static final String FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES = "flow-custom-span-name-and-attributes";

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;
  private final String tracingLevel;
  private final int expectedSpansCount;
  private final Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever;

  private ExportedSpanSniffer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void doAfter() {
    spanCapturer.dispose();
    // TODO W-13160648: Add a Rule for selecting LEVEL of tracing in integration test and make it work in parallel
    clearProperty(TRACING_LEVEL_CONFIGURATION_PATH);
  }

  @Override
  protected String getConfigFile() {
    return "tracing/custom-span-name-and-attributes.xml";
  }

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 2, getOverviewExpectedSpanTestHierarchy()},
        {DEBUG.name(), 2, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans) -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_SOURCE_SPAN_NAME);
      return expectedSpanHierarchy;
    };
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return (exportedSpans) -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_SOURCE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_CUSTOM_SPAN_NAME)
          .endChildren();

      CapturedExportedSpan capturedExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_CUSTOM_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span with customSpanName found!"));

      assertThat(capturedExportedSpan.getAttributes(), hasEntry("attributeAddedByAddCurrentSpanAttribute", "ok"));
      assertThat(capturedExportedSpan.getAttributes(), hasEntry("attributeAddedByAddCurrentSpanAttributes", "ok"));

      CapturedExportedSpan sourceExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_SOURCE_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No source exported span found!"));

      assertThat(sourceExportedSpan.getAttributes(), hasEntry("dog", "Jack, the legendary fake border collie"));

      return expectedSpanHierarchy;
    };
  }

  public CustomSpanNameAndAttributesTestCaseOpenTelemetry(String tracingLevel, int expectedSpansCount,
                                                          Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> spanHierarchyRetriever) {
    this.tracingLevel = tracingLevel;
    this.expectedSpansCount = expectedSpansCount;
    this.spanHierarchyRetriever = spanHierarchyRetriever;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    setProperty(TRACING_LEVEL_CONFIGURATION_PATH, tracingLevel.toLowerCase() + FileSystems.getDefault().getSeparator());
    super.doSetUpBeforeMuleContextCreation();
  }

  private static Function<Collection<CapturedExportedSpan>, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
    // In this case debug and monitoring level are the same.
    return getMonitoringExpectedSpanTestHierarchy();
  }

  @Test
  public void testCustomSpanNameAndAttributes() throws Exception {
    startFlow(FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES);

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
        return exportedSpans.size() == expectedSpansCount;
      }

      @Override
      public String describeFailure() {
        return "No spans were captured";
      }
    });

    spanHierarchyRetriever.apply(spanCapturer.getExportedSpans()).assertSpanTree();
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
