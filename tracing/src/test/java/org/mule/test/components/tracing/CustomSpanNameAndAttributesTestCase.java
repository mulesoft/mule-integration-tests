/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class CustomSpanNameAndAttributesTestCase extends MuleArtifactFunctionalTestCase
    implements TracingTestRunnerConfigAnnotation {

  public static final String EXPECTED_SOURCE_SPAN_NAME = "pet-store-list-modified";
  public static final String EXPECTED_CUSTOM_SPAN_NAME = "customSpanName";
  public static final String FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES = "flow-custom-span-name-and-attributes";

  private static final int TIMEOUT_MILLIS = 5000;
  private static final int POLL_DELAY_MILLIS = 100;

  private ExportedSpanSniffer spanCapturer;

  @Inject
  PrivilegedProfilingService profilingService;

  @Before
  public void initialize() {
    spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();
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
    startFlow(FLOW_CUSTOM_SPAN_NAME_AND_ATTRIBUTES);

    PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

    prober.check(new JUnitProbe() {

      @Override
      protected boolean test() {
        Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
        assertThat(exportedSpans, hasSize(2));
        return true;
      }

      @Override
      public String describeFailure() {
        return "No spans were captured";
      }
    });

    Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
    SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
    expectedSpanHierarchy.withRoot(EXPECTED_SOURCE_SPAN_NAME)
        .beginChildren()
        .child(EXPECTED_CUSTOM_SPAN_NAME)
        .endChildren();

    expectedSpanHierarchy.assertSpanTree();

    CapturedExportedSpan capturedExportedSpan =
        exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_CUSTOM_SPAN_NAME))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("No span with customSpanName found!"));

    assertThat(capturedExportedSpan.getAttributes(), hasEntry("attributeAddedByAddCurrentSpanAttribute", "ok"));
    assertThat(capturedExportedSpan.getAttributes(), hasEntry("attributeAddedByAddCurrentSpanAttributes", "ok"));

    CapturedExportedSpan sourceExportedSpan =
        exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_SOURCE_SPAN_NAME))
            .findFirst()
            .orElseThrow(() -> new AssertionFailedError("No source exported span found!"));

    assertThat(sourceExportedSpan.getAttributes(), hasEntry("dog", "Jack, the legendary fake border collie"));
  }

  private void startFlow(String flowName) throws Exception {
    ((Startable) getFlowConstruct(flowName)).start();
  }
}
