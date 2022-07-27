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

  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String SIMPLE_FLOW = "simple-flow";
  public static final String CORRELATION_ID_KEY = "correlationId";
  public static final String ARTIFACT_TYPE_KEY = "artifactType";
  public static final String ARTIFACT_ID_KEY = "artifactId";
  public static final String THREAD_START_ID_KEY = "threadStartId";
  public static final String ARTIFACT_ID = "SimpleTracingTestCase#testSimpleFlowWithOneProcessor";
  public static final String FLOW_LOCATION = "simple-flow";
  public static final String LOCATION_KEY = "location";
  public static final String SET_PAYLOAD_LOCATION = "simple-flow/processors/0";


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
      assertThat(exportedSpans, hasSize(2));
      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);
      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst().orElse(null);
      assertThat(setPayloadSpan, notNullValue());
      assertThat(muleFlowSpan, notNullValue());
      assertThat(setPayloadSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(muleFlowSpan.getAttributes().get(CORRELATION_ID_KEY), notNullValue());
      assertThat(muleFlowSpan.getAttributes().get(ARTIFACT_TYPE_KEY), equalTo(APP.getAsString()));
      assertThat(muleFlowSpan.getAttributes().get(ARTIFACT_ID_KEY), equalTo(ARTIFACT_ID));
      assertThat(muleFlowSpan.getAttributes().get(THREAD_START_ID_KEY), notNullValue());
      assertThat(muleFlowSpan.getAttributes().get(LOCATION_KEY), equalTo(FLOW_LOCATION));
      assertThat(setPayloadSpan.getAttributes().get(LOCATION_KEY), equalTo(SET_PAYLOAD_LOCATION));
    } finally {
      spanCapturer.dispose();
    }
  }
}
