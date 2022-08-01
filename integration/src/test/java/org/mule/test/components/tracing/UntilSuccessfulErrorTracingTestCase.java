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
public class UntilSuccessfulErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:until-successful:route";
  public static final String EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME = "mule:until-successful";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String UNTIL_SUCCESSFUL_FLOW = "until-successful-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final int NUMBER_OF_RETRIES = 2;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/until-successful-error.xml";
  }

  @Test
  public void testUntilSuccessfulFlowWithError() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(UNTIL_SUCCESSFUL_FLOW).withPayload(TEST_PAYLOAD).dispatch();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan untilSuccessfulSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME)).findFirst()
              .orElse(null);

      List<CapturedExportedSpan> muleRouteSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> loggerSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).collect(Collectors.toList());

      List<CapturedExportedSpan> httpRequestSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .collect(Collectors.toList());

      assertThat(exportedSpans, hasSize((NUMBER_OF_RETRIES + 1) * 3 + 2));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(untilSuccessfulSpan, notNullValue());
      assertThat(untilSuccessfulSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleRouteSpanList, hasSize(NUMBER_OF_RETRIES + 1));
      muleRouteSpanList
          .forEach(muleRouteSpan -> assertThat(muleRouteSpan.getParentSpanId(), equalTo(untilSuccessfulSpan.getSpanId())));
      assertThat(loggerSpanList, hasSize(NUMBER_OF_RETRIES + 1));
      assertThat(httpRequestSpanList, hasSize(NUMBER_OF_RETRIES + 1));
      for (int i = 0; i < NUMBER_OF_RETRIES; i++) {
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
