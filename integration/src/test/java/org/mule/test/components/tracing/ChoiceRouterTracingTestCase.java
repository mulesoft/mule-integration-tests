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
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.getDefaultAttributesToAssertExistence;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;
import java.util.List;

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
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
    List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

    try {
      if (isError) {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).withProfilingService(profilingService).runExpectingException();
      } else {
        flowRunner(CHOICE_FLOW).withPayload(childExpectedSpan).withProfilingService(profilingService).run().getMessage();
      }
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      if (isError) {
        assertThat(exportedSpans, hasSize(5));
      } else {
        assertThat(exportedSpans, hasSize(4));
      }

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("choice-flow", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_CHOICE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("choice-flow/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_ROUTE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap("choice-flow/processors/0", TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(childExpectedSpan)
          .endChildren()
          .endChildren();
      if (isError) {
        expectedSpanHierarchy.child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
            .addAttributesToAssertValue(createAttributeMap("unknown", TEST_ARTIFACT_ID))
            .addAttributesToAssertExistence(attributesToAssertExistence);
      }
      expectedSpanHierarchy.endChildren();

      expectedSpanHierarchy.assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
