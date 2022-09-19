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

import org.mule.functional.api.flow.FlowRunner;
import org.mule.runtime.core.privileged.event.BaseEventContext;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class AsyncSuccessTracingTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_ASYNC_SPAN_NAME = "mule:async";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";
  public static final String EXPECTED_SET_VARIABLE_SPAN_NAME = "mule:set-variable";
  public static final String ASYNC_FLOW = "async-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String NO_PARENT_SPAN = "0000000000000000";

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/async-success.xml";
  }

  @Test
  public void testAsyncFlow() throws Exception {
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      CountDownLatch asyncTerminationLatch = new CountDownLatch(1);
      FlowRunner runner = flowRunner(ASYNC_FLOW).withPayload(TEST_PAYLOAD);
      ((BaseEventContext) (runner.buildEvent().getContext())).onTerminated((e, t) -> asyncTerminationLatch.countDown());
      runner.run();

      asyncTerminationLatch.await();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      CapturedExportedSpan muleFlowSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_FLOW_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan asyncSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_ASYNC_SPAN_NAME)).findFirst()
              .orElse(null);


      CapturedExportedSpan setPayloadSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_PAYLOAD_SPAN_NAME)).findFirst().orElse(null);

      CapturedExportedSpan loggerSpan =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_LOGGER_SPAN_NAME)).findFirst().orElse(null);

      List<CapturedExportedSpan> setVariableSpanList =
          exportedSpans.stream().filter(span -> span.getName().equals(EXPECTED_SET_VARIABLE_SPAN_NAME))
              .collect(Collectors.toList());

      assertThat(exportedSpans, hasSize(6));
      assertThat(muleFlowSpan.getParentSpanId(), equalTo(NO_PARENT_SPAN));
      assertThat(asyncSpan, notNullValue());
      assertThat(asyncSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId()));
      assertThat(setVariableSpanList, hasSize(2));
      setVariableSpanList
          .forEach(setVariableSpan -> assertThat(setVariableSpan.getParentSpanId(), equalTo(muleFlowSpan.getSpanId())));
      assertThat(setPayloadSpan, notNullValue());
      assertThat(setPayloadSpan.getParentSpanId(), equalTo(asyncSpan.getSpanId()));
      assertThat(loggerSpan, notNullValue());
      assertThat(loggerSpan.getParentSpanId(), equalTo(asyncSpan.getSpanId()));
    } finally {
      spanCapturer.dispose();
    }
  }
}
