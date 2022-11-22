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

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class SimpleTracingTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME = "tracing:with-correlation-id";
  private static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  private static final String EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME = "tracing:set-logging-variable";
  private static final String SIMPLE_FLOW = "simple-flow";
  public static final String CORRELATION_ID_KEY = "correlation.id";
  private static final String TEST_ARTIFACT_ID = "SimpleTracingTestCase#testSimpleFlow";
  private static final String FLOW_LOCATION = "simple-flow";
  private static final String SET_PAYLOAD_LOCATION = "simple-flow/processors/2";
  private static final String SET_VARIABLE_LOCATION = "simple-flow/processors/0/processors/0";
  private static final String SET_LOGGING_VARIABLE_LOCATION = "simple-flow/processors/1";
  private static final String TRACING_SET_CORRELATION_ID_LOCATION = "simple-flow/processors/0";
  public static final String TEST_VAR_NAME = "testVar";
  public static final String TRACE_VAR_VALUE = "Hello World!";
  public static final String CORRELATION_ID_CUSTOM_VALUE = "Fua";


  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/flow.xml";
  }

  @Test
  public void testSimpleFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(SIMPLE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      assertThat(exportedSpans, hasSize(5));

      List<String> attributesToAssertExistence = getDefaultAttributesToAssertExistence();

      Map<String, String> setPayloadAttributeMap = createAttributeMap(SET_PAYLOAD_LOCATION, TEST_ARTIFACT_ID);
      setPayloadAttributeMap.put(TEST_VAR_NAME, TRACE_VAR_VALUE);
      Map<String, String> setVariableAttributeMap = createAttributeMap(SET_VARIABLE_LOCATION, TEST_ARTIFACT_ID);
      setVariableAttributeMap.put(CORRELATION_ID_KEY, CORRELATION_ID_CUSTOM_VALUE);

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(FLOW_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(TRACING_SET_CORRELATION_ID_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .beginChildren()
          .child(EXPECTED_SET_VARIABLE_SPAN_NAME)
          .addAttributesToAssertValue(setVariableAttributeMap)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren()
          .child(EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME)
          .addAttributesToAssertValue(createAttributeMap(SET_LOGGING_VARIABLE_LOCATION, TEST_ARTIFACT_ID))
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .addAttributesToAssertValue(setPayloadAttributeMap)
          .addAttributesToAssertExistence(attributesToAssertExistence)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
      exportedSpans.forEach(span -> assertThat(span.getServiceName(), equalTo(span.getAttributes().get(ARTIFACT_ID_KEY))));
    } finally {
      spanCapturer.dispose();
    }
  }
}
