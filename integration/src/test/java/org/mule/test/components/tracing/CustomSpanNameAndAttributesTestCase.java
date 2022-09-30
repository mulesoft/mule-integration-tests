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

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CustomSpanNameAndAttributesTestCase extends AbstractIntegrationTestCase {

  public static final String FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES = "flow-custom-span-name-and-attributes";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  public static final String EXPECTED_CUSTOM_SPAN_NAME = "customSpanName";

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
    Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
    assertThat(exportedSpans, hasSize(2));

    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_CUSTOM_SPAN_NAME)
        .endChildren();

    expectedSpanHierarchy.assertSpanTree(expectedSpanHierarchy.getRoot());

    CapturedExportedSpan capturedExportedSpan =
        spanCapturer.getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_CUSTOM_SPAN_NAME))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("No span with customSpanName found!"));
    assertThat(capturedExportedSpan.getAttributes(), Matchers.hasEntry("attributeAddedByAddCurrentSpanAttribute", "ok"));
    assertThat(capturedExportedSpan.getAttributes(), Matchers.hasEntry("attributeAddedByAddCurrentSpanAttributes", "ok"));
  }
}
