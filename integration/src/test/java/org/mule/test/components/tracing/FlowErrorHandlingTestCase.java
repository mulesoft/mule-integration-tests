/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import junit.framework.AssertionFailedError;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FlowErrorHandlingTestCase extends AbstractIntegrationTestCase {

  public static final String ROOT_PARENT_SPAN_ID = "0000000000000000";

  private static final String FLOW_WITH_NO_ERROR_HANDLING = "flow-with-no-error-handling";
  private static final String FLOW_WITH_ON_ERROR_CONTINUE = "flow-with-on-error-continue";
  private static final String FLOW_WITH_ON_ERROR_PROPAGATE = "flow-with-on-error-propagate";
  private static final String FLOW_WITH_FLOW_REF_AND_NO_ERROR_HANDLING = "flow-with-flow-ref-and-no-error-handling";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_ERROR_HANDLING = "flow-with-flow-ref-and-on-error-continue";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_ERROR_HANDLING = "flow-with-flow-ref-and-on-error-propagate";
  private static final String FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_ERROR_HANDLING = "flow-with-flow-ref-and-on-error-propagate-and-on-error-continue";
  private static final String FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_ERROR_HANDLING = "flow-with-sub-flow-ref-and-on-error-continue";

  private static final String FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING = "flow-with-sub-flow-ref-and-no-error-handling";


  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:flow-ref/mule:flow/mule:raise-error",
          "mule:flow/mule:flow-ref/mule:flow/mule:on-error-propagate",
          "mule:flow/mule:on-error-propagate"
  ));

  private static final Set<String> FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:raise-error",
          "mule:flow/mule:on-error-propagate"
  ));

  private static final Set<String> FLOW_WITH_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:raise-error",
          "mule:flow/mule:on-error-continue"
  ));

  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:flow-ref/mule:flow/mule:raise-error",
          "mule:flow/mule:flow-ref/mule:flow/mule:on-error-continue"
  ));

  private static final Set<String> FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:flow-ref/mule:flow/mule:raise-error",
          "mule:flow/mule:flow-ref/mule:flow/mule:on-error-propagate",
          "mule:flow/mule:on-error-continue"
  ));

  private static final Set<String> FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:flow-ref/mule:flow-ref:route/mule:raise-error",
          "mule:flow/mule:on-error-propagate"
  ));

  private static final Set<String> FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES = new HashSet<>(Arrays.asList(
          "mule:flow/mule:flow-ref/mule:flow-ref:route/mule:raise-error",
          "mule:flow/mule:on-error-continue"
  ));

  public static final String SPAN_BRANCH_DELIMITATOR = "/";


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
  public void testFlowWithNoErrorHandling() {
    try {
      flowRunner(FLOW_WITH_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES);
    }
  }

  @Test
  public void testFlowWithOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES);
  }

  @Test
  public void testFlowWithOnErrorPropagate() {
    try {
      flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpanBranches(FLOW_WITH_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES);
    }
  }

  @Test
  public void testFlowWithFlowRefAndNoErrorHandling() {
    try {
      flowRunner(FLOW_WITH_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES);
    }
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorContinue() throws Exception {
      flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
      assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES);
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagate() {
    try {
      flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_EXPECTED_SPAN_BRANCHES);
    }
  }

  @Test
  public void testFlowWithFlowRefAndOnErrorPropagateAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_FLOW_REF_AND_ON_ERROR_PROPAGATE_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES);
  }

  @Test
  public void testFlowWithSubFlowRefAndNoErrorHandling() {
    try {
      flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpanBranches(FLOW_WITH_SUB_FLOW_REF_AND_NO_ERROR_HANDLING_EXPECTED_SPAN_BRANCHES);
    }
  }

  @Test
  public void testFlowWithSubFlowRefAndOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_ERROR_HANDLING).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpanBranches(FLOW_WITH_SUB_FLOW_REF_AND_ON_ERROR_CONTINUE_EXPECTED_SPAN_BRANCHES);
  }

  private void assertExpectedSpanBranches(Set<String> expectedSpanBranches) {
    expectedSpanBranches.forEach(this::assertExpectedSpanBranch);
  }

  private void assertExpectedSpanBranch(String expectedSpanBranch) {
    String[] branchSpanNames = expectedSpanBranch.split(SPAN_BRANCH_DELIMITATOR);
    CapturedExportedSpan currentSpan = findRootSpan(branchSpanNames[0]);
    for (int i = 1; i < branchSpanNames.length; i ++) {
      try {
        currentSpan = assertExpectedSpan(branchSpanNames[i], currentSpan.getSpanId());
      } catch (Throwable t) {
        throw new AssertionError(String.format("Expected Span branch not found: [%s]", expectedSpanBranch), t);
      }
    }
  }

  private CapturedExportedSpan assertExpectedSpan(String branchSpanName, String parentSpanId) {
    return spanCapturer.getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getName().equals(branchSpanName) && exportedSpan.getParentSpanId().equals(parentSpanId))
            .findFirst().orElseThrow(() -> new AssertionFailedError(String.format("Expected Span with name: [%s] and parent ID [%s] not found", branchSpanName, parentSpanId)));
  }

  private CapturedExportedSpan findRootSpan(String rootSpanName) {
    return spanCapturer.getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getName().equals(rootSpanName) && exportedSpan.getParentSpanId().equals(ROOT_PARENT_SPAN_ID)).findFirst()
            .orElseThrow(() -> new AssertionFailedError(String.format("Root Span with name: [%s] not found.", rootSpanName)));
  }
}
