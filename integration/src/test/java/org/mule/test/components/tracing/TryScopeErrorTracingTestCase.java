/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasProperty;
import static org.hamcrest.Matchers.is;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.components.tracing.TracingTestUtils.assertSpanAttributes;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
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
public class TryScopeErrorTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String TRY_SCOPE_FLOW = "try-scope-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_TRY_SCOPE_SPAN_NAME = "mule:try";
  public static final String EXPECTED_TRY_SCOPE_ROUTE_SPAN_NAME = "mule:try:route";
  public static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTEFACT_ID = "TryScopeErrorTracingTestCase#testTryScope";
  public static final String PARENT_SPAN_ID_PROPERTY_NAME = "parentSpanId";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/try-scope-error.xml";
  }

  @Test
  public void testTryScope() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(TRY_SCOPE_FLOW).withPayload(TEST_PAYLOAD).runExpectingException();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan tryScope =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_TRY_SCOPE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan tryScopeRoute =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_TRY_SCOPE_ROUTE_SPAN_NAME)).findFirst()
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

      List<CapturedExportedSpan> onErrorPropagateSpans =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME))
              .collect(Collectors.toList());

      assertThat(exportedSpans, hasSize(7));

      assertThat(muleFlowSpan, notNullValue());
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));

      assertThat(tryScope, notNullValue());

      assertThat(tryScope.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));

      assertThat(tryScopeRoute, notNullValue());
      assertThat(tryScopeRoute.getParentSpanId(), equalTo(tryScope.getSpanId()));

      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(tryScopeRoute.getSpanId()));

      assertThat(raiseErrorSpan, notNullValue());
      assertThat(raiseErrorSpan.getParentSpanId(), equalTo(tryScopeRoute.getSpanId()));

      assertThat(setPayloadSpan, nullValue());

      assertThat(onErrorPropagateSpans.size(), equalTo(2));
      assertThat(onErrorPropagateSpans,
                 containsInAnyOrder(hasProperty(PARENT_SPAN_ID_PROPERTY_NAME, equalTo(tryScopeRoute.getSpanId())),
                                    hasProperty("parentSpanId", equalTo(muleFlowSpan.getSpanId()))));

      assertSpanAttributes(muleFlowSpan, "try-scope-flow", TEST_ARTEFACT_ID);
      assertSpanAttributes(tryScope, "try-scope-flow/processors/0", TEST_ARTEFACT_ID);
      assertSpanAttributes(tryScopeRoute, "try-scope-flow/processors/0", TEST_ARTEFACT_ID);
      assertSpanAttributes(loggerSpan, "try-scope-flow/processors/0/processors/0", TEST_ARTEFACT_ID);
      assertSpanAttributes(raiseErrorSpan, "try-scope-flow/processors/0/processors/1", TEST_ARTEFACT_ID);
    } finally {
      spanCapturer.dispose();
    }
  }
}
