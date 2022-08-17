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
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CustomScopeErrorTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_CUSTOM_SCOPE_SPAN_NAME = "heisenberg:execute-anything";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_CUSTOM_SCOPE_ROUTE_SPAN_NAME = "heisenberg:execute-anything:route";
  public static final String CUSTOM_SCOPE_FLOW = "custom-scope-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  private static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";


  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/custom-scope-error.xml";
  }

  @Test
  public void testCustomScopeFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(CUSTOM_SCOPE_FLOW).withPayload(TEST_PAYLOAD).runExpectingException();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan customScopeSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_CUSTOM_SCOPE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan customScopeRoute =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_CUSTOM_SCOPE_ROUTE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan raiseErrorSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_RAISE_ERROR_SPAN_NAME)).findFirst()
              .orElse(null);


      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan onErrorPropagateSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)).findFirst()
              .orElse(null);

      assertThat(exportedSpans, hasSize(6));

      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));

      assertThat(customScopeSpan, notNullValue());
      assertThat(customScopeSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));

      assertThat(customScopeRoute, notNullValue());
      assertThat(customScopeRoute.getParentSpanId(), equalTo(customScopeSpan.getSpanId()));

      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(customScopeRoute.getSpanId()));

      assertThat(raiseErrorSpan, notNullValue());
      assertThat(raiseErrorSpan.getParentSpanId(), equalTo(customScopeRoute.getSpanId()));

      assertThat(onErrorPropagateSpan, notNullValue());
      assertThat(onErrorPropagateSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));

      assertThat(setPayloadSpan, nullValue());

    } finally {
      spanCapturer.dispose();
    }
  }
}

