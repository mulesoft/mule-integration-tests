/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;
import static java.lang.String.format;
import static java.util.Arrays.stream;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.util.Pair;
import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.jetbrains.annotations.NotNull;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class FlowErrorHandlingTestCase extends AbstractIntegrationTestCase {

  private static final Pair<String, String> EXPECTED_RAISE_ERROR_SPAN = new Pair<>("mule:raise-error", "mule:flow");
  private static final Pair<String, String> EXPECTED_FLOW_SPAN = new Pair<>("mule:flow", "");
  private static final String FLOW_WITH_ON_ERROR_CONTINUE = "flow-with-on-error-continue";
  private static final String FLOW_WITH_ON_ERROR_PROPAGATE = "flow-with-on-error-propagate";

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
  public void testFlowWithOnErrorContinue() throws Exception {
    flowRunner(FLOW_WITH_ON_ERROR_CONTINUE).withPayload(TEST_PAYLOAD).run().getMessage();
    assertExpectedSpans(EXPECTED_FLOW_SPAN, EXPECTED_RAISE_ERROR_SPAN);
  }

  @Test
  public void testFlowWithOnErrorPropagate() {
    try {
      flowRunner(FLOW_WITH_ON_ERROR_PROPAGATE).withPayload(TEST_PAYLOAD).run().getMessage();
    } catch (Throwable e) {
      // Nothing to do (the flow under test is propagating an error).
    } finally {
      assertExpectedSpans(EXPECTED_FLOW_SPAN, EXPECTED_RAISE_ERROR_SPAN);
    }
  }

  @SafeVarargs
  private final void assertExpectedSpans(Pair<String, String>... expectedSpans) {
    Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
    Collection<Matcher<CapturedExportedSpan>> expectedSpanMatchers =
        stream(expectedSpans).map(this::getSpanMatcher).collect(Collectors.toList());
    expectedSpanMatchers.forEach(spanMatcher -> assertThat(exportedSpans, hasItem(spanMatcher)));
    // TODO : Show the unexpected spans as has part of the error
    assertThat("Unexpected spans have been exported.", exportedSpans.size(), equalTo(expectedSpanMatchers.size()));
  }

  @NotNull
  private SpanMatcher getSpanMatcher(Pair<String, String> expectedSpan) {
    return new SpanMatcher(expectedSpan.getFirst(),
                           expectedSpan.getSecond().isEmpty() ? "" : findExportedSpan(expectedSpan.getSecond()).getSpanId());
  }

  @Nonnull
  private CapturedExportedSpan findExportedSpan(String spanName) {
    return spanCapturer.getExportedSpans().stream().filter(exportedSpan -> exportedSpan.getName().equals(spanName)).findFirst()
        .orElseThrow(() -> new RuntimeException(format("Expected Span with name [%s] not found.", spanName)));
  }

  /**
   * A CapturedExportedSpan Hamcrest matcher that can be reused for other tests if necessary.
   */
  public static class SpanMatcher extends BaseMatcher<CapturedExportedSpan> {

    private static final String ROOT_PARENT_SPAN_ID = "0000000000000000";
    private final String spanName;
    private final String parentSpanId;

    public SpanMatcher(String spanName, String parentSpanId) {
      this.spanName = spanName;
      this.parentSpanId = parentSpanId.equals("") ? ROOT_PARENT_SPAN_ID : parentSpanId;
    }

    @Override
    public boolean matches(Object actual) {
      if (!(actual instanceof CapturedExportedSpan)) {
        return false;
      }
      CapturedExportedSpan potentialMatch = (CapturedExportedSpan) actual;
      return potentialMatch.getName().equals(spanName) && potentialMatch.getParentSpanId().equals(parentSpanId);
    }

    @Override
    public void describeTo(Description description) {
      description.appendText(format("a Span with name: [%s] and parent Span ID: [%s].", spanName, parentSpanId));
    }
  }
}
