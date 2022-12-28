/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static org.mule.test.infrastructure.profiling.tracing.TracingTestUtils.createAttributeMap;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.AbstractMuleTestCase;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FlowErrorHandlingTracingTestCase extends MuleArtifactFunctionalTestCase
    implements TracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String ERROR_TYPE_1 = "CUSTOM:ERROR";
  public static final String ERROR_TYPE_2 = "CUSTOM:ERROR_2";

  public static final String EXPECTED_ROUTE_SPAN_NAME = "mule:round-robin:route";
  public static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN = "mule:on-error-propagate";
  public static final String EXPECTED_ON_ERROR_CONTINUE_SPAN = "mule:on-error-continue";
  public static final String EXPECTED_FLOW_REF_SPAN = "mule:flow-ref";
  public static final String EXPECTED_SUBFLOW_SPAN = "mule:subflow";
  public static final String NO_PARENT_SPAN = "0000000000000000";
  public static final String TEST_ARTIFACT_ID =
      "FlowErrorHandlingTracingTestCase#flowWIthOnErrorPropagateAndOnErrorContinueComposition";

  private static final String FLOW_WITH_NO_ERROR_HANDLING = "flow-with-no-error-handling";
  private static final String FLOW_WITH_ON_ERROR_CONTINUE = "flow-with-on-error-continue";
  private static final String FLOW_WITH_ON_ERROR_PROPAGATE = "flow-with-on-error-propagate";
  private static final String FLOW_WITH_FLOW_REF_AND_NO_ERROR_HANDLING = "flow-with-flow-ref-and-no-error-handling";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE =
      "flow-with-flow-ref-and-on-error-continue";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE =
      "flow-with-flow-ref-and-on-error-propagate";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE =
      "flow-with-flow-ref-and-on-error-propagate-and-on-error-continue";
  private static final String FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE =
      "flow-with-sub-flow-ref-and-on-error-continue";
  private static final String FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING = "flow-with-sub-flow-ref-and-no-error-handling";
  private static final String FLOW_WITH_FAILING_ON_ERROR_CONTINUE = "flow-with-failing-on-error-continue";
  private static final String FLOW_WITH_FAILING_ON_ERROR_PROPAGATE = "flow-with-failing-on-error-propagate";
  private static final String FLOW_WITH_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_COMPOSITION =
      "flow-with-on-error-propagate-and-on-error-continue-composition";

  private ExportedSpanSniffer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/flow-error-handling.xml";
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
  public void testFlowWithNoErrorHandling() throws Exception {

    flowRunner(FLOW_WITH_NO_ERROR_HANDLING).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));

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

  @Test
  public void testFlowWithOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_CONTINUE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run().getMessage();

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
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_2)
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithFailingOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FAILING_ON_ERROR_CONTINUE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR_2"));

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 4;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_2)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).addExceptionData(ERROR_TYPE_2)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_2)
        .endChildren()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithFailingOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_FAILING_ON_ERROR_PROPAGATE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR_2"));

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 4;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_2)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).addExceptionData(ERROR_TYPE_2)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_2)
        .endChildren()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
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

  @Test
  public void testFlowWithFlowRefAndNoErrorHandling() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 6;
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
        .child(EXPECTED_FLOW_REF_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren()
        .endChildren()
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run()
        .getMessage();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 5;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_REF_SPAN).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_2)
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren()
        .endChildren()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 6;
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
        .child(EXPECTED_FLOW_REF_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren()
        .endChildren()
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagateAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE)
        .withPayload(AbstractMuleTestCase.TEST_PAYLOAD).run()
        .getMessage();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 6;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_REF_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_FLOW_SPAN_NAME).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren()
        .endChildren()
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithSubFlowRefAndNoErrorHandling() throws Exception {
    flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 5;
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
        .child(EXPECTED_FLOW_REF_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_SUBFLOW_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .endChildren()
        .endChildren()
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void testFlowWithSubFlowRefAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE).withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .run().getMessage();

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 5;
      }

      @Override
      public String describeFailure() {
        return "The exact amount of spans was not captured";
      }
    });

    Collection<CapturedExportedSpan> capturedExportedSpans = spanCapturer.getExportedSpans();

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(capturedExportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_REF_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_SUBFLOW_SPAN).addExceptionData(ERROR_TYPE_1)
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN).addExceptionData(ERROR_TYPE_1)
        .endChildren()
        .endChildren()
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }

  @Test
  public void flowWIthOnErrorPropagateAndOnErrorContinueComposition() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_COMPOSITION)
        .withPayload(AbstractMuleTestCase.TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
        return exportedSpans.size() == 7;
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
        .child(EXPECTED_RAISE_ERROR_SPAN)
        .addAttributesToAssertValue(createAttributeMap("flow-with-on-error-propagate-and-on-error-continue-composition/processors/0",
                                                       TEST_ARTIFACT_ID))
        .addExceptionData(ERROR_TYPE_1)
        .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_REF_SPAN).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_FLOW_SPAN_NAME).noExceptionExpected()
        .beginChildren()
        .child(EXPECTED_RAISE_ERROR_SPAN)
        .addAttributesToAssertValue(createAttributeMap("flow-with-on-error-continue/processors/0", TEST_ARTIFACT_ID))
        .addExceptionData(ERROR_TYPE_2)
        .child(EXPECTED_ON_ERROR_CONTINUE_SPAN).noExceptionExpected()
        .endChildren()
        .endChildren()
        .endChildren()
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();
  }
}
