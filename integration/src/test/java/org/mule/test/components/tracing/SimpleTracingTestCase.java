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
    } finally {
      spanCapturer.dispose();
    }
  }
}
