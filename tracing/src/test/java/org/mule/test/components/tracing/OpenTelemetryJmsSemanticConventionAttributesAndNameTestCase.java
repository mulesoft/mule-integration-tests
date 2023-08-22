/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static java.lang.String.format;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.tracer.api.sniffer.CapturedExportedSpan;
import org.mule.runtime.tracer.api.sniffer.ExportedSpanSniffer;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.probe.JUnitProbe;
import org.mule.tck.probe.PollingProber;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import org.junit.Ignore;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class OpenTelemetryJmsSemanticConventionAttributesAndNameTestCase extends OpenTelemetryTracingSnifferTestCase
    implements OpenTelemetryTracingTestRunnerConfigAnnotation {

  private static final int TIMEOUT_MILLIS = 30000;
  private static final int POLL_DELAY_MILLIS = 100;

  public static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "GET /";
  public static final String EXPECTED_JMS_PUBLISH_NAME = "test_queue send";
  public static final String EXPECTED_JMS_CONSUME_NAME = "test_queue receive";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";

  public static final String MESSAGING_SYSTEM = "messaging.system";
  public static final String MESSAGING_DESTINATION = "messaging.destination";
  public static final String MESSAGING_DESTINATION_KIND = "messaging.destination_kind";
  public static final String SPAN_KIND_ATTRIBUTE = "span.kind.override";

  @Inject
  PrivilegedProfilingService profilingService;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "tracing/jms-semantic-conventions-tracing.xml";
  }

  @Test
  public void testFlow() throws IOException, TimeoutException {
    ExportedSpanSniffer spanCapturer = profilingService.getSpanExportManager().getExportedSpanSniffer();

    try {
      httpClient.send(HttpRequest.builder().uri(String.format("http://localhost:%s/", httpPort.getNumber())).build());

      PollingProber prober = new PollingProber(TIMEOUT_MILLIS, POLL_DELAY_MILLIS);

      prober.check(new JUnitProbe() {

        @Override
        protected boolean test() {
          Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();;
          return exportedSpans.size() == 4;
        }

        @Override
        public String describeFailure() {
          return format("The exact amount of spans was not captured. Captured spans: %s",
                        spanCapturer.getExportedSpans().toString());
        }
      });

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();

      Map<String, String> jmsExpectedAttributes = new HashMap<>();
      jmsExpectedAttributes.put(MESSAGING_SYSTEM, "activemq");
      jmsExpectedAttributes.put(MESSAGING_DESTINATION, "test_queue");
      jmsExpectedAttributes.put(MESSAGING_DESTINATION_KIND, "queue");

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .child(EXPECTED_JMS_PUBLISH_NAME)
          .addAttributesToAssertValue(jmsExpectedAttributes)
          .child(EXPECTED_JMS_CONSUME_NAME)
          .addAttributesToAssertValue(jmsExpectedAttributes)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
      CapturedExportedSpan jmsPublishSpan = exportedSpans.stream()
          .filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_JMS_PUBLISH_NAME)).findFirst().get();
      CapturedExportedSpan jmsConsumeSpan = exportedSpans.stream()
          .filter(exportedSpan -> exportedSpan.getName().equals(EXPECTED_JMS_CONSUME_NAME)).findFirst().get();

      assertThat(jmsPublishSpan.getSpanKindName(), equalTo("PRODUCER"));
      assertThat(jmsPublishSpan.getAttributes().size(), equalTo(10));
      assertThat(jmsPublishSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
      assertThat(jmsConsumeSpan.getSpanKindName(), equalTo("CONSUMER"));
      assertThat(jmsConsumeSpan.getAttributes().size(), equalTo(10));
      assertThat(jmsConsumeSpan.getAttributes().get(SPAN_KIND_ATTRIBUTE), nullValue());
    } finally {
      spanCapturer.dispose();
    }
  }
}
