/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.util.Collections.singletonList;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;

import org.hamcrest.Matchers;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.HashSet;
import java.util.Set;
import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CustomSpanNameAndAttributesTestCase extends AbstractIntegrationTestCase {

  public static final String FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES = "flow-custom-span-name-and-attributes";

  // TODO: This should be removed and adapted according to W-11573985: Make tracing tests more declarative

  public static final String ROOT_PARENT_SPAN_ID = "0000000000000000";

  private static final String SPAN_BRANCH_DELIMITATOR = "/";
  public static final String SPAN_ATTRIBUTES_BEGIN = "[";

  private static final Set<String> FLOW_EXPECTED_SPANS =
      new HashSet<>(singletonList(
                                  "mule:flow/customSpanName"));

  private ExportedSpanCapturer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();
  }

  @After
  public void dispose() {
    spanCapturer.dispose();
  }

  @Override
  protected String getConfigFile() {
    return "tracing/custom-span-name-and-attributes.xml";
  }

  @Test
  public void testCustomSpanNameAndAttributes() throws Exception {
    flowRunner(FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES).run().getContext();
    assertExpectedSpanBranches();
    CapturedExportedSpan capturedExportedSpan =
        spanCapturer.getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getName().equals("customSpanName"))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("No span with customSpanName found!"));
    assertThat(capturedExportedSpan.getAttributes(), Matchers.hasEntry("attributeAddedByAddCurrentSpanAttribute", "ok"));
    assertThat(capturedExportedSpan.getAttributes(), Matchers.hasEntry("attributeAddedByAddCurrentSpanAttributes", "ok"));
  }

  // TODO: This should be removed and adapted according to W-11573985: Make tracing tests more declarative
  private void assertExpectedSpanBranches() {
    FLOW_EXPECTED_SPANS.forEach(this::assertExpectedSpanBranch);
    assertThat("Additional tracing Spans has been found.", spanCapturer.getExportedSpans().size(), equalTo(2));
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
    return spanCapturer.getExportedSpans().stream()
        .filter(exportedSpan -> exportedSpan.getName().equals(spanName)
            && exportedSpan.getParentSpanId().equals(parentSpanId))
        .findFirst().orElseThrow(() -> new AssertionFailedError(String
            .format(
                    "Expected tracing Span with name: [%s] and parent ID [%s] not found",
                    spanName, parentSpanId)));
  }

  private String getSpanName(String branchSpanName) {
    if (branchSpanName.contains(SPAN_ATTRIBUTES_BEGIN)) {
      return branchSpanName.substring(0, branchSpanName.indexOf(SPAN_ATTRIBUTES_BEGIN));
    } else {
      return branchSpanName;
    }
  }
}
