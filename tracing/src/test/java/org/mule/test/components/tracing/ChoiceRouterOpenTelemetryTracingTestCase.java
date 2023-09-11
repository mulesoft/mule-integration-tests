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

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;
import org.mule.test.runner.RunnerDelegateTo;

import java.nio.file.FileSystems;
import java.util.Collection;
import java.util.List;
import java.util.function.BiFunction;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.runners.Parameterized;
import org.junit.After;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
@RunnerDelegateTo(Parameterized.class)
public class ChoiceRouterOpenTelemetryTracingTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:choice:route";
  public static final String EXPECTED_CHOICE_SPAN_NAME = "mule:choice";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  public static final String CHOICE_FLOW = "choice-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";

  public static final String TEST_ARTIFACT_ID = "ChoiceRouterOpenTelemetryTracingTestCase#testChoiceFlow";
  private final String tracingLevel;
  private final int expectedSuccessSpansCount;
  private final BiFunction<Collection<CapturedExportedSpan>, SpanTestHierarchyParameters, SpanTestHierarchy> spanHierarchyRetriever;
  private final int expectedErrorSpansCount;

  @Parameterized.Parameters(name = "tracingLevel: {0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {OVERVIEW.name(), 1, 1, getOverviewExpectedSpanTestHierarchy()},
        {MONITORING.name(), 4, 5, getMonitoringExpectedSpanTestHierarchy()},
        {DEBUG.name(), 4, 5, getDebugExpectedSpanTestHierarchy()}
    });
  }

  private static BiFunction<Collection<CapturedExportedSpan>, SpanTestHierarchyParameters, SpanTestHierarchy> getOverviewExpectedSpanTestHierarchy() {
    return (exportedSpans, spanTestHierarchyParameters) -> {
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME);
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy = expectedSpanHierarchy.addExceptionData("ANY:EXPECTED");
      }
      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, SpanTestHierarchyParameters, SpanTestHierarchy> getMonitoringExpectedSpanTestHierarchy() {
    return (exportedSpans, spanTestHierarchyParameters) -> {
      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy = expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("choice-flow", spanTestHierarchyParameters.getArtifactId()))
          .addAttributesToAssertExistence(attributesToAssertExistence);
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy = expectedSpanHierarchy.addExceptionData("ANY:EXPECTED");
      }
      expectedSpanHierarchy = expectedSpanHierarchy.beginChildren()
          .child(EXPECTED_CHOICE_SPAN_NAME);
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy = expectedSpanHierarchy.addExceptionData("ANY:EXPECTED");
      }
      expectedSpanHierarchy = expectedSpanHierarchy
          .addAttributesToAssertValue(createAttributeMap("choice-flow/processors/0", spanTestHierarchyParameters.getArtifactId()))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME);
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy = expectedSpanHierarchy.addExceptionData("ANY:EXPECTED");
      }
      expectedSpanHierarchy = expectedSpanHierarchy
          .addAttributesToAssertValue(createAttributeMap("choice-flow/processors/0", spanTestHierarchyParameters.getArtifactId()))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(spanTestHierarchyParameters.getChildSpanName());
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy = expectedSpanHierarchy.addExceptionData("ANY:EXPECTED");
      }
      expectedSpanHierarchy = expectedSpanHierarchy.endChildren()
          .endChildren();
      if (spanTestHierarchyParameters.isError()) {
        expectedSpanHierarchy.child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
            .addAttributesToAssertValue(createAttributeMap("unknown", spanTestHierarchyParameters.getArtifactId()))
            .addAttributesToAssertExistence(attributesToAssertExistence);
      }
      expectedSpanHierarchy.endChildren();

      return expectedSpanHierarchy;
    };
  }

  private static BiFunction<Collection<CapturedExportedSpan>, SpanTestHierarchyParameters, SpanTestHierarchy> getDebugExpectedSpanTestHierarchy() {
    // In this case debug and monitoring level are the same.
    return getMonitoringExpectedSpanTestHierarchy();
  }

  public ChoiceRouterOpenTelemetryTracingTestCase(String tracingLevel, int expectedSuccessSpansCount, int expectedErrorSpansCount,
                                                  BiFunction<Collection<CapturedExportedSpan>, SpanTestHierarchyParameters, SpanTestHierarchy> spanHierarchyRetriever) {
    this.tracingLevel = tracingLevel;
    this.expectedSuccessSpansCount = expectedSuccessSpansCount;
    this.expectedErrorSpansCount = expectedErrorSpansCount;
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

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/choice-router.xml";
  }

  @Test
  public void testChoiceFlow() throws Exception {
    testForRoute(EXPECTED_LOGGER_SPAN_NAME, false);
    testForRoute(EXPECTED_SET_PAYLOAD_SPAN_NAME, false);
    testForRoute(EXPECTED_RAISE_ERROR_SPAN_NAME, true);
  }

  private void testForRoute(String childExpectedSpan, boolean isError) throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      if (isError) {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).runExpectingException();
      } else {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).run().getMessage();
      }

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          if (isError) {
            return exportedSpans.size() == expectedErrorSpansCount;
          } else {
            return exportedSpans.size() == expectedSuccessSpansCount;
          }
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      String artifactId = TEST_ARTIFACT_ID + "[tracingLevel: " + tracingLevel + "]";
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();


      spanHierarchyRetriever.apply(exportedSpans, new SpanTestHierarchyParameters(artifactId, childExpectedSpan, isError))
          .assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span
          .getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }

  /**
   * Parameters for the asserting the expected {@link SpanTestHierarchy}.
   */
  private final static class SpanTestHierarchyParameters {

    private final String artifactId;
    private final String childSpan;
    private final boolean isError;

    private SpanTestHierarchyParameters(String artifactId, String childSpan, boolean isError) {
      this.artifactId = artifactId;
      this.childSpan = childSpan;
      this.isError = isError;
    }

    public String getArtifactId() {
      return artifactId;
    }

    public String getChildSpanName() {
      return childSpan;
    }

    public boolean isError() {
      return isError;
    }
  }
}
