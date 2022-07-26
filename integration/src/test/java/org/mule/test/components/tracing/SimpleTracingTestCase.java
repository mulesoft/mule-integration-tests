/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
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
public class SimpleTracingTestCase extends AbstractIntegrationTestCase {

  private static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  private static final String EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME = "tracing:with-correlation-id";
  private static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  private static final String EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME = "tracing:set-logging-variable";
  private static final String NO_PARENT_SPAN = "0000000000000000";
  private static final String SIMPLE_FLOW = "simple-flow";
  private static final String CORRELATION_ID_KEY = "correlationId";
  private static final String ARTIFACT_TYPE_KEY = "artifactType";
  private static final String ARTIFACT_ID_KEY = "artifactId";
  private static final String THREAD_START_ID_KEY = "threadStartId";
  private static final String ARTIFACT_ID = "SimpleTracingTestCase#testSimpleFlowWithOneProcessor";
  private static final String FLOW_LOCATION = "simple-flow";
  private static final String LOCATION_KEY = "location";
  private static final String SET_PAYLOAD_LOCATION = "simple-flow/processors/2";
  private static final String SET_VARIABLE_LOCATION = "simple-flow/processors/0/processors/0";
  private static final String SET_LOGGGING_VARIABLE_LOCATION = "simple-flow/processors/1";
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
  public void testSimpleFlowWithOneProcessor() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      flowRunner(SIMPLE_FLOW).withPayload(TEST_PAYLOAD).run().getMessage();
      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      assertThat(exportedSpans, hasSize(5));
      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);
      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst().orElse(null);
      CapturedExportedSpan tracingCorrelationidSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_TRACING_CORRELATION_ID_SPAN_NAME)).findFirst()
              .orElse(null);
      CapturedExportedSpan setVariableSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_VARIABLE_SPAN_NAME)).findFirst().orElse(null);
      CapturedExportedSpan setLoggingVariable =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_LOGGING_VARIABLE_SPAN_NAME)).findFirst()
              .orElse(null);
      assertThat(setPayloadSpan, notNullValue());
      assertThat(muleFlowSpan, notNullValue());
      assertSpan(setVariableSpan, tracingCorrelationidSpan, SET_VARIABLE_LOCATION);
      assertSpan(tracingCorrelationidSpan, muleFlowSpan, TRACING_SET_CORRELATION_ID_LOCATION);
      assertSpan(setLoggingVariable, muleFlowSpan, SET_LOGGGING_VARIABLE_LOCATION);
      assertSpan(setPayloadSpan, muleFlowSpan, SET_PAYLOAD_LOCATION);
      assertSpan(muleFlowSpan, null, FLOW_LOCATION);
      assertThat(setPayloadSpan.getAttributes().get(TEST_VAR_NAME), equalTo(TRACE_VAR_VALUE));
      assertThat(setVariableSpan.getAttributes().get(CORRELATION_ID_KEY), equalTo(CORRELATION_ID_CUSTOM_VALUE));
    } finally {
      spanCapturer.dispose();
    }
  }

  private void assertSpan(CapturedExportedSpan spam, CapturedExportedSpan parentSpan, String location) {
    assertThat(spam.getParentSpanId(), equalTo(parentSpan == null ? NO_PARENT_SPAN : parentSpan.getSpanId()));
    assertThat(spam.getAttributes().get(LOCATION_KEY), equalTo(location));
    assertThat(spam.getAttributes().get(CORRELATION_ID_KEY), notNullValue());
    assertThat(spam.getAttributes().get(ARTIFACT_TYPE_KEY), equalTo(APP.getAsString()));
    assertThat(spam.getAttributes().get(ARTIFACT_ID_KEY), equalTo(ARTIFACT_ID));
    assertThat(spam.getAttributes().get(THREAD_START_ID_KEY), notNullValue());
  }
}
