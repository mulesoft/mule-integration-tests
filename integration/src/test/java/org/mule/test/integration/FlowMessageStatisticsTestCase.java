/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.PRICING_METRICS;
import static org.mule.test.allure.AllureConstants.PricingMetricsFeature.MessageMetricsStory.LAPSED_MESSAGE_METRICS;

import static java.lang.String.format;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.management.stats.FlowConstructStatistics;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(PRICING_METRICS)
@Story(LAPSED_MESSAGE_METRICS)
public class FlowMessageStatisticsTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  private FlowConstructStatistics applicationStatistics;

  boolean statisticsEnabledOriginal;

  @Before
  public void before() {
    statisticsEnabledOriginal = muleContext.getStatistics().isEnabled();
    muleContext.getStatistics().setEnabled(true);
  }

  @After
  public void after() {
    muleContext.getStatistics().setEnabled(statisticsEnabledOriginal);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/flow-message-statistics-config.xml";
  }

  @Before
  public void setUp() {
    applicationStatistics = muleContext.getStatistics().getApplicationStatistics();
  }

  @Test
  public void withSource() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri(format("http://localhost:%s/withSource",
                    port.getNumber()))
        .method(POST)
        .build();

    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    long totalMessagesDispatched = applicationStatistics.getTotalDispatchedMessages();
    long totalEventsReceived = applicationStatistics.getTotalEventsReceived();

    assertThat(totalMessagesDispatched, is(1L));
    assertThat(totalEventsReceived, is(1L));
  }

  @Test
  public void withSourceAndFlowRef() throws Exception {
    HttpRequest request = HttpRequest.builder()
        .uri(format("http://localhost:%s/withSourceAndFlowRef",
                    port.getNumber()))
        .method(POST)
        .build();

    httpClient.send(request, RECEIVE_TIMEOUT, false, null);

    long totalMessagesDispatched = applicationStatistics.getTotalDispatchedMessages();
    long totalEventsReceived = applicationStatistics.getTotalEventsReceived();

    assertThat(totalMessagesDispatched, is(1L));
    assertThat(totalEventsReceived, is(2L));
  }
}
