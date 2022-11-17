/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.ARTIFACT_ID_KEY;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

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
  public static final String EXPECTED_RAISE_ERROR_SPAN_NAME = "mule:raise-error";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTIFACT_ID = "TryScopeErrorTracingTestCase#testTryScope";

  public static final String CORRELATION_ID_KEY = "correlation.id";
  public static final String THREAD_START_ID_KEY = "thread.start.id";

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

      assertThat(exportedSpans, hasSize(6));

      List<String> attributesToAssertExistence = Arrays.asList(CORRELATION_ID_KEY, THREAD_START_ID_KEY);

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("try-scope-flow", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_TRY_SCOPE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("try-scope-flow/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("try-scope-flow/processors/0/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_RAISE_ERROR_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("try-scope-flow/processors/0/processors/1", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren()
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
