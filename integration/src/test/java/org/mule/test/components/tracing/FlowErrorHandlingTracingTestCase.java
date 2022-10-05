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

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.privileged.profiling.CapturedEventData;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FlowErrorHandlingTracingTestCase extends AbstractIntegrationTestCase {

  public static final String ROOT_PARENT_SPAN_ID = "0000000000000000";

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

  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                                  "mule:flow[exception: CUSTOM:ERROR]/mule:flow-ref[exception: CUSTOM:ERROR]/mule:flow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                                                                                                                  "mule:flow[exception: CUSTOM:ERROR]/mule:flow-ref[exception: CUSTOM:ERROR]/mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate",
                                                                                                                                  "mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate"));

  private static final Set<String> FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                     "mule:flow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                                                                                                     "mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate"));

  private static final Set<String> FLOW_WITH_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                    "mule:flow/mule:raise-error[exception: CUSTOM:ERROR_2]",
                                                                                                                    "mule:flow/mule:on-error-continue"));

  private static final Set<String> FLOW_WITH_FAILING_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                            "mule:flow[exception: CUSTOM:ERROR_2]/mule:raise-error[exception: CUSTOM:ERROR]",
                                                                                                                            "mule:flow[exception: CUSTOM:ERROR_2]/mule:on-error-continue[exception: CUSTOM:ERROR_2]/mule:raise-error[exception: CUSTOM:ERROR_2]"));

  private static final Set<String> FLOW_WITH_FAILING_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                             "mule:flow[exception: CUSTOM:ERROR_2]/mule:raise-error[exception: CUSTOM:ERROR]",
                                                                                                                             "mule:flow[exception: CUSTOM:ERROR_2]/mule:on-error-propagate[exception: CUSTOM:ERROR_2]/mule:raise-error[exception: CUSTOM:ERROR_2]"));

  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
                                                                                                                                 "mule:flow/mule:flow-ref/mule:flow/mule:raise-error[exception: CUSTOM:ERROR_2]",
                                                                                                                                 "mule:flow/mule:flow-ref/mule:flow/mule:on-error-continue"));

  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES =
      new HashSet<>(Arrays.asList(
                                  "mule:flow/mule:flow-ref[exception: CUSTOM:ERROR]/mule:flow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                  "mule:flow/mule:flow-ref[exception: CUSTOM:ERROR]/mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate",
                                  "mule:flow/mule:on-error-continue"));

  private static final Set<String> FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING_EXPECTED_SPAN_BRANCHES =
      new HashSet<>(Arrays.asList(
                                  "mule:flow[exception: CUSTOM:ERROR]/mule:flow-ref[exception: CUSTOM:ERROR]/mule:subflow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                  "mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate"));

  private static final Set<String> FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES =
      new HashSet<>(Arrays.asList(
                                  "mule:flow/mule:flow-ref[exception: CUSTOM:ERROR]/mule:subflow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                  "mule:flow/mule:on-error-continue"));

  private static final Set<String> FLOW_WITH_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_COMPOSITION_EXPECTED_SPAN_BRANCHES =
      new HashSet<>(Arrays.asList(
                                  "mule:flow[exception: CUSTOM:ERROR]/mule:raise-error[exception: CUSTOM:ERROR]",
                                  "mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate/mule:flow-ref/mule:flow/mule:raise-error[exception: CUSTOM:ERROR_2]",
                                  "mule:flow[exception: CUSTOM:ERROR]/mule:on-error-propagate/mule:flow-ref/mule:flow/mule:on-error-continue"));

  private static final String SPAN_BRANCH_DELIMITATOR = "/";
  public static final String SPAN_ATTRIBUTES_BEGIN = "[";
  public static final String SPAN_ATTRIBUTES_END = "]";
  public static final String EXCEPTION_ATTRIBUTE = "exception:";

  public static final String OTEL_EXCEPTION_TYPE_KEY = "exception.type";
  public static final String OTEL_EXCEPTION_MESSAGE_KEY = "exception.message";
  public static final String OTEL_EXCEPTION_STACK_TRACE_KEY = "exception.stacktrace";
  public static final String OTEL_EXCEPTION_ESCAPED_KEY = "exception.escaped";
  public static final String OTEL_EXCEPTION_EVENT_NAME = "exception";

  private ExportedSpanCapturer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Override
  protected String getConfigFile() {
    return "tracing/flow-error-handling.xml";
  }

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
  }

  @Test
  public void testFlowWithNoErrorHandling() throws Exception {
    flowRunner(FLOW_WITH_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD).runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES, 3);
  }

  @Test
  public void testFlowWithOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES, 3);
  }

  @Test
  public void testFlowWithFailingOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FAILING_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR_2"));
    assertExpectedSpanBranches(FLOW_WITH_FAILING_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES, 4);
  }

  @Test
  public void testFlowWithFailingOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_FAILING_ON_ERROR_PROPAGATE).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR_2"));
    assertExpectedSpanBranches(FLOW_WITH_FAILING_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES, 4);
  }

  @Test
  public void testFlowWithOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE).withPayload(TEST_PAYLOAD).runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES, 3);
  }

  @Test
  public void testFlowWithFlowRefAndNoErrorHandling() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES, 6);
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES, 5);
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagate() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES, 6);
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagateAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run()
        .getMessage();
    assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES, 6);
  }

  @Test
  public void testFlowWithSubFlowRefAndNoErrorHandling() throws Exception {
    flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING_EXPECTED_SPAN_BRANCHES, 5);
  }

  @Test
  public void testFlowWithSubFlowRefAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES, 5);
  }

  @Test
  public void flowWIthOnErrorPropagateAndOnErrorContinueComposition() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_COMPOSITION).withPayload(TEST_PAYLOAD)
        .runExpectingException(errorType("CUSTOM", "ERROR"));
    assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_COMPOSITION_EXPECTED_SPAN_BRANCHES, 7);
  }

  private void assertExpectedSpanBranches(Set<String> expectedSpanBranches, int totalSpans) {
    expectedSpanBranches.forEach(this::assertExpectedSpanBranch);
    assertThat("Additional tracing Spans has been found.", spanCapturer.getExportedSpans().size(), equalTo(totalSpans));
  }

  private void assertExpectedSpanBranch(String expectedSpanBranch) {
    String[] branchSpanNames = expectedSpanBranch.split(SPAN_BRANCH_DELIMITATOR);
    CapturedExportedSpan currentSpan = null;
    for (String branchSpanName : branchSpanNames) {
      try {
        currentSpan = assertExpectedSpan(branchSpanName, currentSpan != null ? currentSpan.getSpanId() : ROOT_PARENT_SPAN_ID);
      } catch (Throwable t) {
        throw new AssertionError(String.format("Expected tracing Span branch not found: [%s]", expectedSpanBranch), t);
      }
    }
  }

  private CapturedExportedSpan assertExpectedSpan(String spanDefinition, String parentSpanId) {
    String spanName = getSpanName(spanDefinition);
    CapturedExportedSpan capturedExportedSpan = spanCapturer.getExportedSpans().stream()
        .filter(exportedSpan -> exportedSpan.getName().equals(spanName)
            && exportedSpan.getParentSpanId().equals(parentSpanId))
        .findFirst().orElseThrow(() -> new AssertionFailedError(String
            .format("Expected tracing Span with name: [%s] and parent ID [%s] not found", spanName, parentSpanId)));
    assertExpectedException(capturedExportedSpan, spanDefinition);
    return capturedExportedSpan;
  }

  private String getSpanName(String branchSpanName) {
    if (branchSpanName.contains(SPAN_ATTRIBUTES_BEGIN)) {
      return branchSpanName.substring(0, branchSpanName.indexOf(SPAN_ATTRIBUTES_BEGIN));
    } else {
      return branchSpanName;
    }
  }

  private void assertExpectedException(CapturedExportedSpan capturedExportedSpan, String branchSpanName) {
    if (branchSpanName.contains(EXCEPTION_ATTRIBUTE)) {
      List<CapturedEventData> exceptions = capturedExportedSpan.getEvents().stream()
          .filter(capturedEventData -> capturedEventData.getName().equals(OTEL_EXCEPTION_EVENT_NAME))
          .collect(Collectors.toList());
      assertThat(String.format("Expected exceptions for Span: [%s] differ", capturedExportedSpan), exceptions.size(), equalTo(1));
      assertExceptionData(exceptions.iterator().next(), getErrorType(branchSpanName));
      assertThat(capturedExportedSpan.hasErrorStatus(), is(true));
    } else {
      assertThat(String.format("Unexpected Span exceptions found for Span: [%s]", capturedExportedSpan),
                 capturedExportedSpan.getEvents().size(), equalTo(0));
    }
  }

  private String getErrorType(String branchSpanName) {
    try {
      return branchSpanName.split(EXCEPTION_ATTRIBUTE)[1].split(SPAN_ATTRIBUTES_END)[0].trim();
    } catch (Throwable t) {
      throw new IllegalArgumentException(String.format("Malformed exception notation at span: %s", branchSpanName));
    }
  }

  private void assertExceptionData(CapturedEventData exceptionData, String errorType) {
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_TYPE_KEY), equalTo(errorType));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_MESSAGE_KEY), equalTo("An error occurred."));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_ESCAPED_KEY), equalTo("true"));
    assertThat(exceptionData.getAttributes().get(OTEL_EXCEPTION_STACK_TRACE_KEY).toString(), not(emptyOrNullString()));
  }
}
