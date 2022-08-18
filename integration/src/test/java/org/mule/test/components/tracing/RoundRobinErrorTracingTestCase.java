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

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
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
public class RoundRobinErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_ROUND_ROBIN_SPAN_NAME = "mule:round-robin";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String ROUND_ROBIN_FLOW = "round-robin-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  public static final String TEST_ARTIFACT_ID = "RoundRobinErrorTracingTestCase#testRoundRobinFlowWithError";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/round-robin-error.xml";
  }

  @Test
  public void testRoundRobinFlowWithError() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(ROUND_ROBIN_FLOW).withPayload(TEST_PAYLOAD).runExpectingException();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan roundRobinSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUND_ROBIN_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> raiseErrorSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_RAISE_ERROR_SPAN))
              .collect(Collectors.toList());
      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(0).getSpanId())).findFirst()
              .orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan onErrorPropagateSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ON_ERROR_PROPAGATE_SPAN)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(6));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(onErrorPropagateSpan, notNullValue());
      assertThat(onErrorPropagateSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(roundRobinSpan, notNullValue());
      assertThat(roundRobinSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(1));

      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(roundRobinSpan.getSpanId())));
      assertThat(raiseErrorSpanList, hasSize(1));
      assertThat(setPayloadSpan, notNullValue());

      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(muleRouteSpanList.get(0).getSpanId()));

      assertSpanAttributes(muleFlowSpan, "round-robin-flow", TEST_ARTIFACT_ID);
      assertSpanAttributes(onErrorPropagateSpan, "unknown", TEST_ARTIFACT_ID);
      assertSpanAttributes(roundRobinSpan, "round-robin-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(muleRouteSpanList.get(0), "round-robin-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(loggerSpan, "round-robin-flow/processors/0/route/0/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(raiseErrorSpanList.get(0), "round-robin-flow/processors/0/route/0/processors/1", TEST_ARTIFACT_ID);
    } finally {
      spanCapturer.dispose();
    }
  }
}