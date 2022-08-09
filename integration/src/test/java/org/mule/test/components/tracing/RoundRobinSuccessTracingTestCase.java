/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class RoundRobinSuccessTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String ROUND_ROBIN_FLOW = "round-robin-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

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

      CapturedExportedSpan roundRobinSuccessfulSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUND_ROBIN_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> setPayloadSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME))
              .collect(Collectors.toList());
      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(0).getSpanId())).findFirst()
              .orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(numberOfExpectedSpans));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(roundRobinSuccessfulSpan, notNullValue());
      assertThat(roundRobinSuccessfulSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(1));

      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(roundRobinSuccessfulSpan.getSpanId())));
      if (verifySetPayloadInRoute) {
        assertThat(setPayloadSpanList, hasSize(1));
        assertThat(setPayloadSpan, notNullValue());
      } else {
        assertThat(setPayloadSpanList, hasSize(0));
      }
      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(muleRouteSpanList.get(0).getSpanId()));
    } finally {
      spanCapturer.dispose();
    }
  }
}

