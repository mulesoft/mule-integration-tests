/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.either;
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
public class FirstSuccessfulErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:first-successful:route";
  public static final String EXPECTED_FIRST_SUCCESSFUL_SPAN_NAME = "mule:first-successful";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String FLOW = "first-successful-telemetryFlow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final int NUMBER_OF_ROUTES = 2;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/first-successful-error.xml";
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(FLOW).withPayload(TEST_PAYLOAD).dispatch();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      List<CapturedExportedSpan> setPayloadSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME))
              .collect(Collectors.toList());

      CapturedExportedSpan firstSuccessfulSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FIRST_SUCCESSFUL_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> loggerSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> setVariableSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_VARIABLE_SPAN_NAME))
              .collect(Collectors.toList());

      List<CapturedExportedSpan> httpRequestSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .collect(Collectors.toList());

      assertThat(exportedSpans, hasSize(3 * NUMBER_OF_ROUTES + 4));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(setVariableSpanList, hasSize(2));
      setVariableSpanList
          .forEach(setVariableSpan -> assertThat(setVariableSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId())));
      assertThat(firstSuccessfulSpan, notNullValue());
      assertThat(firstSuccessfulSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(NUMBER_OF_ROUTES));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(firstSuccessfulSpan.getSpanId())));
      assertThat(loggerSpanList, hasSize(1));
      assertThat(loggerSpanList.get(0).getParentSpanId(),
                 either(equalTo(muleRouteSpanList.get(0).getSpanId())).or(equalTo(muleRouteSpanList.get(1).getSpanId())));
      assertThat(httpRequestSpanList, hasSize(1));
      assertThat(httpRequestSpanList.get(0).getParentSpanId(),
                 either(equalTo(muleRouteSpanList.get(0).getSpanId())).or(equalTo(muleRouteSpanList.get(1).getSpanId())));
      assertThat(setPayloadSpanList, hasSize(NUMBER_OF_ROUTES));
      for (int i = 0; i < NUMBER_OF_ROUTES; i++) {
        int finalI = i;
        CapturedExportedSpan setPayloadSpan =
            setPayloadSpanList.stream().filter(span -> span.getParentSpanId().equals(muleRouteSpanList.get(finalI).getSpanId()))
                .findFirst().orElse(null);
        assertThat(setPayloadSpan, notNullValue());
      }
    } finally {
      spanCapturer.dispose();
    }
  }
}
