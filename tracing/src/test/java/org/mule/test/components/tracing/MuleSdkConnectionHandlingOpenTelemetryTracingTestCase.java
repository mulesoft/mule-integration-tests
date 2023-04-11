/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import javax.inject.Inject;
import java.util.Collection;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class MuleSdkConnectionHandlingOpenTelemetryTracingTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String ERROR_TYPE_1 = "CUSTOM:ERROR";

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN = "mule:on-error-propagate";

  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTIFACT_ID =
      "FlowErrorHandlingOpenTelemetryTracingTestCase#flowWIthOnErrorPropagateAndOnErrorContinueComposition";

  private static final String OPERATION_WITH_SIMPLE_CONNECTION = "operation-with-simple-connection";

  private ExportedSpanSniffer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/mule-sdk-connection-handling.xml";
  }

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
  }

  @Test
  public void testOperationWithSimpleConnection() throws Exception {

    flowRunner(OPERATION_WITH_SIMPLE_CONNECTION).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 3;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });


    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();


    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

}
