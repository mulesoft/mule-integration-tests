/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class UntilSuccessfulErrorTracingTestCase extends MuleArtifactFunctionalTestCase
    implements TracingTestRunnerConfigAnnotation {

  public static final String EXPECTED_ATTEMPT_SPAN_NAME = "mule:until-successful:attempt";
  public static final String EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME = "mule:until-successful";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String UNTIL_SUCCESSFUL_FLOW = "until-successful-flow";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "http:request";
  public static final String ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final int NUMBER_OF_RETRIES = 2;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/until-successful-error.xml";
  }

  @Test
  public void testUntilSuccessfulFlowWithError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(UNTIL_SUCCESSFUL_FLOW).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).dispatch();

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      assertThat(exportedSpans, hasSize((NUMBER_OF_RETRIES + 1) * 3 + 3));

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_UNTIL_SUCCESSFUL_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_ATTEMPT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .endChildren()
          .child(EXPECTED_ATTEMPT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .endChildren()
          .child(EXPECTED_ATTEMPT_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .endChildren()
          .endChildren()
          .child(ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }
}
