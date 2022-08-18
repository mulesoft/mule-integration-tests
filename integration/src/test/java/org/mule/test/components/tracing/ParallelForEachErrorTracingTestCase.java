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
public class ParallelForEachErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:parallel-foreach:route";
  public static final String EXPECTED_PARALLEL_FOREACH_SPAN_NAME = "mule:parallel-foreach";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String PARALLEL_FOR_EACH_FLOW = "parallel-for-eachFlow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final int NUMBER_OF_ROUTES = 3;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/parallel-foreach-error.xml";
  }

  @Test
  public void testFlowWithError() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(PARALLEL_FOR_EACH_FLOW).withPayload(TEST_PAYLOAD).dispatch();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan parallelForEachSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_PARALLEL_FOREACH_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> loggerSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> httpRequestSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .collect(Collectors.toList());

      CapturedExportedSpan onErrorPropagateSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)).findFirst()
              .orElse(null);

      assertThat(exportedSpans, hasSize(NUMBER_OF_ROUTES * 3 + 4));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(onErrorPropagateSpan, notNullValue());
      assertThat(onErrorPropagateSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(setPayloadSpan, notNullValue());
      assertThat(setPayloadSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(parallelForEachSpan, notNullValue());
      assertThat(parallelForEachSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(NUMBER_OF_ROUTES));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(parallelForEachSpan.getSpanId())));
      assertThat(loggerSpanList, hasSize(NUMBER_OF_ROUTES));
      assertThat(httpRequestSpanList, hasSize(NUMBER_OF_ROUTES));
      for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
        int finalI = i;
        CapturedExportedSpan loggerSpan =
            loggerSpanList.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(finalI).getSpanId()))
                .findFirst().orElse(null);
        assertThat(loggerSpan, notNullValue());
        CapturedExportedSpan httpRequestSpan =
            httpRequestSpanList.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(finalI).getSpanId()))
                .findFirst().orElse(null);
        assertThat(httpRequestSpan, notNullValue());
      }
    } finally {
      spanCapturer.dispose();
    }
  }
}