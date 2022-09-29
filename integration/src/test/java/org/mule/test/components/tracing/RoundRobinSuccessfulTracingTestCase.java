/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.components.tracing.TracingTestUtils.assertSpanAttributes;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.SpanTestHierarchy;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class RoundRobinSuccessfulTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String ROUND_ROBIN_FLOW = "round-robin-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  public static final String TEST_ARTIFACT_ID = "RoundRobinSuccessfulTracingTestCase#testRoundRobinFlow";

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
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanCapturer(), 5, true);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanCapturer(), 4, false);
    assertRoundRobinSpan(profilingService.getSpanExportManager().getExportedSpanCapturer(), 5, true);
  }

  private void assertRoundRobinSpan(ExportedSpanCapturer spanCapturer, int numberOfExpectedSpans, boolean verifySetPayloadInRoute)
      throws Exception {
    try {
      flowRunner(ROUND_ROBIN_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan roundRobinSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUND_ROBIN_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> setPayloadSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME))
              .collect(Collectors.toList());

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(numberOfExpectedSpans));

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME);
      if (verifySetPayloadInRoute) {
        expectedSpanHierarchy
            .child(EXPECTED_SET_PAYLOAD_SPAN_NAME);
      }
      expectedSpanHierarchy
          .endChildren()
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertSpanTree(expectedSpanHierarchy.getRoot(), null);

      assertSpanAttributes(muleFlowSpan, "round-robin-flow", TEST_ARTIFACT_ID);
      assertSpanAttributes(roundRobinSpan, "round-robin-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(muleRouteSpanList.get(0), "round-robin-flow/processors/0", TEST_ARTIFACT_ID);

      if (verifySetPayloadInRoute) {
        assertSpanAttributes(loggerSpan, "round-robin-flow/processors/0/route/0/processors/0", TEST_ARTIFACT_ID);
        assertSpanAttributes(setPayloadSpanList.get(0), "round-robin-flow/processors/0/route/0/processors/1", TEST_ARTIFACT_ID);
      } else {
        assertSpanAttributes(loggerSpan, "round-robin-flow/processors/0/route/1/processors/0", TEST_ARTIFACT_ID);
      }
    } finally {
      spanCapturer.dispose();
    }
  }
}

