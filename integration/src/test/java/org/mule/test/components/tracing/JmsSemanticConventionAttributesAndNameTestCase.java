/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

import static org.mule.test.allure.AllureConstants.Profiling.PROFILING;
import static org.mule.test.allure.AllureConstants.Profiling.ProfilingServiceStory.DEFAULT_CORE_EVENT_TRACER;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;
import org.mule.runtime.core.privileged.profiling.ExportedSpanCapturer;
import org.mule.runtime.core.privileged.profiling.PrivilegedProfilingService;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.infrastructure.profiling.tracing.SpanTestHierarchy;

import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(PROFILING)
@Story(DEFAULT_CORE_EVENT_TRACER)
public class JmsSemanticConventionAttributesAndNameTestCase extends AbstractIntegrationTestCase {

  public static final String EXPECTED_HTTP_FLOW_SPAN_NAME = "/";
  public static final String EXPECTED_JMS_PUBLISH_NAME = "test_queue send";
  public static final String EXPECTED_JMS_CONSUME_NAME = "test_queue receive";
  private static final String EXPECTED_SET_PAYLOAD_SPAN_NAME = "mule:set-payload";

  public static final String MESSAGING_SYSTEM = "messaging.system";
  public static final String MESSAGING_DESTINATION = "messaging.destination";
  public static final String MESSAGING_DESTINATION_KIND = "messaging.destination_kind";
  public static final String MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES = "messaging.message_payload_size_bytes";

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
    ExportedSpanCapturer spanCapturer = profilingService.getSpanExportManager().getExportedSpanCapturer();

    try {
      httpClient.send(HttpRequest.builder().uri(String.format("http://localhost:%s/", httpPort.getNumber())).build());

      Collection<CapturedExportedSpan> exportedSpans = spanCapturer.getExportedSpans();
      assertThat(exportedSpans, hasSize(4));

      Map<String, String> jmsPublishExpectedAttributes = new HashMap<>();
      jmsPublishExpectedAttributes.put(MESSAGING_SYSTEM, "activemq");
      jmsPublishExpectedAttributes.put(MESSAGING_DESTINATION, "test_queue");
      jmsPublishExpectedAttributes.put(MESSAGING_DESTINATION_KIND, "queue");
      jmsPublishExpectedAttributes.put(MESSAGING_MESSAGE_PAYLOAD_SIZE_BYTES, "7");

      Map<String, String> jmsConsumeExpectedAttributes = new HashMap<>();
      jmsConsumeExpectedAttributes.put(MESSAGING_SYSTEM, "activemq");
      jmsConsumeExpectedAttributes.put(MESSAGING_DESTINATION, "test_queue");
      jmsConsumeExpectedAttributes.put(MESSAGING_DESTINATION_KIND, "queue");

      SpanTestHierarchy expectedSpanHierarchy = new SpanTestHierarchy(exportedSpans);
      expectedSpanHierarchy.withRoot(EXPECTED_HTTP_FLOW_SPAN_NAME)
          .beginChildren()
          .child(EXPECTED_SET_PAYLOAD_SPAN_NAME)
          .child(EXPECTED_JMS_PUBLISH_NAME)
          .addAttributesToAssertValue(jmsPublishExpectedAttributes)
          .child(EXPECTED_JMS_CONSUME_NAME)
          .addAttributesToAssertValue(jmsConsumeExpectedAttributes)
          .endChildren();

      expectedSpanHierarchy.assertSpanTree();
    } finally {
      spanCapturer.dispose();
    }
  }

}
