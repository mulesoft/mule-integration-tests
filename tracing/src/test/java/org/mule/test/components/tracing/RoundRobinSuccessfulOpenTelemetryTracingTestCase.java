/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.ARTIFACT_ID_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
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

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/round-robin-success.xml";
  }

  @Test
  public void testRoundRobinFlow() throws Exception {
    // We send three requests to verify that the tracing to verify the round robin functioning
    // and that the traces corresponds to that.
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), 5, true);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), 4, false);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanSniffer(), 5, true);
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

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      Map<String, String> loggerSpanAttributeMap;
      if (verifySetPayloadInRoute) {
        loggerSpanAttributeMap = createAttributeMap("round-robin-flow/processors/0/route/0/processors/0", TEST_ARTIFACT_ID);
      } else {
        loggerSpanAttributeMap = createAttributeMap("round-robin-flow/processors/0/route/1/processors/0", TEST_ARTIFACT_ID);
      }

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(loggerSpanAttributeMap)
          .addAttributesToAssertExistence(attributesToAssertExistence);
      if (verifySetPayloadInRoute) {
        expectedSpanHierarchy
            .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
            .addAttributesToAssertValue(createAttributeMap("round-robin-flow/processors/0/route/0/processors/1",
                                                           TEST_ARTIFACT_ID))
            .addAttributesToAssertExistence(attributesToAssertExistence);
      }
      expectedSpanHierarchy
          .endChildren()
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}

