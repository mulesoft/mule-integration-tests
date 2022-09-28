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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class TryScopeSuccessfulTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String TRY_SCOPE_FLOW = "try-scope-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_TRY_SCOPE_SPAN_NAME = "mule:try";
  public static final String EXPECTED_TRY_SCOPE_ROUTE_SPAN_NAME = "mule:try:route";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTEFACT_ID = "TryScopeSuccessfulTracingTestCase#testTryScope";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/try-scope-successful.xml";
  }

  @Test
  public void testTryScope() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(TRY_SCOPE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      assertThat(exportedSpans, hasSize(4));

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

      Map<String, String> tryScopeAttributes = new HashMap<>();
      
      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_TRY_SCOPE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_TRY_SCOPE_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertRoot(expectedSpanHierarchy.getRoot(), muleFlowSpan);
      expectedSpanHierarchy.assertPreOrder(expectedSpanHierarchy.getRoot(), muleFlowSpan);

      assertSpanAttributes(muleFlowSpan, "try-scope-flow", TEST_ARTEFACT_ID);
      assertSpanAttributes(tryScope, "try-scope-flow/processors/0", TEST_ARTEFACT_ID);
      assertSpanAttributes(tryScopeRoute, "try-scope-flow/processors/0", TEST_ARTEFACT_ID);
      assertSpanAttributes(loggerSpan, "try-scope-flow/processors/0/processors/0", TEST_ARTEFACT_ID);
    } finally {
      spanCapturer.dispose();
    }
  }
}
