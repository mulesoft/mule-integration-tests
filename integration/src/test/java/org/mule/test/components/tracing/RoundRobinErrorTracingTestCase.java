/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;

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

      assertThat(exportedSpans, hasSize(6));

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME, createAttributeMap("round-robin-flow", TEST_ARTIFACT_ID))
          .beginChildren()
          .child(EXPECTED_ROUND_ROBIN_SPAN_NAME, createAttributeMap("round-robin-flow/processors/0", TEST_ARTIFACT_ID))
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME, createAttributeMap("round-robin-flow/processors/0", TEST_ARTIFACT_ID))
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME,
                 createAttributeMap("round-robin-flow/processors/0/route/0/processors/0", TEST_ARTIFACT_ID))
          .child(EXPECTED_RAISE_ERROR_SPAN,
                 createAttributeMap("round-robin-flow/processors/0/route/0/processors/1", TEST_ARTIFACT_ID))
          .endChildren()
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN, createAttributeMap("unknown", TEST_ARTIFACT_ID))
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }
}
