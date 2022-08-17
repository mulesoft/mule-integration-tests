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
public class ScatterGatherSuccessTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:scatter-gather:route";
  public static final String EXPECTED_SCATTER_GATHER_SPAN_NAME = "mule:scatter-gather";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String SCATTER_GATHER_FLOW = "scatter-gather-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/scatter-gather-success.xml";
  }

  @Test
  public void testScatterGatherFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(SCATTER_GATHER_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan scatterGatherSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SCATTER_GATHER_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> setPayloadSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME))
              .collect(Collectors.toList());
      CapturedExportedSpan setPayloadFirstCapturedRouteSpan =
          setPayloadSpanList.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(0).getSpanId()))
              .findFirst().orElse(null);
      CapturedExportedSpan setPayloadSecondCapturedRouteSpan =
          setPayloadSpanList.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(1).getSpanId()))
              .findFirst().orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      assertThat(exportedSpans, hasSize(7));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(scatterGatherSpan, notNullValue());
      assertThat(scatterGatherSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(2));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(scatterGatherSpan.getSpanId())));
      assertThat(setPayloadSpanList, hasSize(2));
      assertThat(setPayloadFirstCapturedRouteSpan, notNullValue());
      assertThat(setPayloadSecondCapturedRouteSpan, notNullValue());
      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
    } finally {
      spanCapturer.dispose();
    }
  }
}

