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
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class ChoiceRouterTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:choice:route";
  public static final String EXPECTED_CHOICE_SPAN_NAME = "mule:choice";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  public static final String CHOICE_FLOW = "choice-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";


  public static final String TEST_ARTIFACT_ID = "ChoiceRouterTracingTestCase#testChoiceFlow";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/choice-router.xml";
  }

  @Test
  public void testChoiceFlow() throws Exception {
    testForRoute(EXPECTED_LOGGER_SPAN_NAME, false);
    testForRoute(EXPECTED_SET_PAYLOAD_SPAN_NAME, false);
    testForRoute(EXPECTED_RAISE_ERROR_SPAN_NAME, true);
  }

  private void testForRoute(String childExpectedSpan, boolean isError) throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      if (isError) {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).runExpectingException();
      } else {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).run().getMessage();
      }
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan choiceSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_CHOICE_SPAN_NAME)).findFirst()
              .orElse(null);

      CapturedExportedSpan choiceSpanRoute =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ROUTE_SPAN_NAME)).findFirst()
              .orElse(null);

      if (isError) {
        assertThat(exportedSpans, hasSize(5));
        CapturedExportedSpan onErrorPropagateSpan =
            exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)).findFirst()
                .orElse(null);
        assertSpanAttributes(onErrorPropagateSpan, "unknown", TEST_ARTIFACT_ID);
      } else {
        assertThat(exportedSpans, hasSize(4));
      }

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_CHOICE_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .beginChildren()
          .child(childExpectedSpan)
          .endChildren()
          .endChildren();
      if (isError) {
        expectedSpanHierarchy.child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME);
      }
      expectedSpanHierarchy.endChildren();

      expectedSpanHierarchy.assertSpanTree(expectedSpanHierarchy.getRoot());

      assertSpanAttributes(muleFlowSpan, "choice-flow", TEST_ARTIFACT_ID);
      assertSpanAttributes(choiceSpan, "choice-flow/processors/0", TEST_ARTIFACT_ID);
      assertSpanAttributes(choiceSpanRoute, "choice-flow/processors/0", TEST_ARTIFACT_ID);
    } finally {
      spanCapturer.dispose();
    }
  }
}
