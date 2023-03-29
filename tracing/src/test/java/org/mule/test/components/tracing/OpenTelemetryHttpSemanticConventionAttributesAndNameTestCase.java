/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.Matchers.aMapWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Collection;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import junit.framework.AssertionFailedError;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class OpenTelemetryHttpSemanticConventionAttributesAndNameTestCase extends MuleArtifactFunctionalTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static final String STARTING_FLOW = "startingFlow";
  private static final String HTTP_LISTENER_ERROR_200_FLOW = "httpListenerErrorButReturns200";
  public static final String EXPECTED_FLOW_SPAN_NAME = "mule:flow";
  private static final String EXPECTED_HTTP_REQUEST_SPAN_NAME = "HTTP GET";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "/test";
  private static final String EXPECTED_HTTP_FLOW_SPAN_NAME_200 = "/test200";
  private static final String EXPECTED_LOGGER_SPAN_NAME = "mule:logger";
  public static final String EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME = "mule:on-error-propagate";
  public static final String EXPECTED_RAISE_ERROR_SPAN = "mule:raise-error";
  public static final String NET_PEER_NAME = "net.peer.name";
  public static final String NET_PEER_PORT = "net.peer.port";
  public static final String HTTP_URL = "http.url";
  public static final String HTTP_METHOD = "http.method";
  public static final String HTTP_FLAVOR = "http.flavor";
  public static final String NET_HOST_NAME = "net.host.name";
  public static final String HTTP_TARGET = "http.target";
  public static final String HTTP_USER_AGENT = "http.user_agent";
  public static final String NET_HOST_PORT = "net.host.port";
  public static final String HTTP_SCHEME = "http.scheme";
  public static final String HTTP_STATUS_CODE = "http.status_code";
  public static final String SPAN_KIND_ATTRIBUTE = "span.kind.override";
  public static final String SPAN_STATUS_ATTRIBUTE = "status.override";

  @Inject
  PrivilegedProfilingService profilingService;

  @Rule
  public DynamicPort httpPort = new DynamicPort("port");

  @Override
  protected String getConfigFile() {
    return "tracing/http-semantic-conventions-tracing.xml";
  }

  @Test
  public void testFlow() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      flowRunner(STARTING_FLOW).run();

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 4;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_LOGGER_SPAN_NAME)
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(15));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo("UNSET"));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, httpPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/test"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("UNSET"));
    } finally {
      spanCapturer.dispose();
    }
  }

  @Test
  public void testWhenHTTPListenerFlowThrowsErrorButReturns200SpanStatusShouldNotBeSetAsError() throws Exception {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);
      flowRunner(HTTP_LISTENER_ERROR_200_FLOW).dispatch();

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
          return exportedSpans.size() == 5;
        }

        @Override
        public String describeFailure() {
          return "The exact amount of spans was not captured";
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_REQUEST_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_HTTP_FLOW_SPAN_NAME_200)
          .beginChildren()
          .child(EXPECTED_RAISE_ERROR_SPAN)
          .child(EXPECTED_ON_ERROR_PROPAGATE_SPAN_NAME)
          .endChildren()
          .endChildren()
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();

      CapturedExportedSpan listenerExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_FLOW_SPAN_NAME_200))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http listener flow found!"));

      assertThat(listenerExportedSpan.getAttributes(), aMapWithSize(15));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_NAME, "0.0.0.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_TARGET, "/test200"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_SCHEME, "http"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_USER_AGENT, "AHC/1.0"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(NET_HOST_PORT, httpPort.getValue()));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(listenerExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(listenerExportedSpan.getSpanKindName(), equalTo("SERVER"));
      assertThat(listenerExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(listenerExportedSpan.getStatusAsString(), equalTo("UNSET"));

      CapturedExportedSpan requestExportedSpan =
          exportedSpans.stream().filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_HTTP_REQUEST_SPAN_NAME))
              .findFirst()
              .orElseThrow(() -> new AssertionFailedError("No span for http request flow found!"));

      assertThat(requestExportedSpan.getAttributes(), aMapWithSize(13));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_NAME, "localhost"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(NET_PEER_PORT, httpPort.getValue()));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_URL, "http://localhost:" + httpPort.getValue() + "/test200"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_METHOD, "GET"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_FLAVOR, "1.1"));
      assertThat(requestExportedSpan.getAttributes(), hasEntry(HTTP_STATUS_CODE, "200"));
      assertThat(requestExportedSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getAttributes().get(SPAN_STATUS_ATTRIBUTE), nullValue());
      assertThat(requestExportedSpan.getSpanKindName(), equalTo("CLIENT"));
      assertThat(requestExportedSpan.hasErrorStatus(), equalTo(false));
      assertThat(requestExportedSpan.getStatusAsString(), equalTo("UNSET"));
    } finally {
      spanCapturer.dispose();
    }
  }
}
