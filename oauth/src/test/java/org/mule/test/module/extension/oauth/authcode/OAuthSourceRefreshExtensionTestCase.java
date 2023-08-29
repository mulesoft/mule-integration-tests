/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.tck.probe.PollingProber.check;

import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.notification.ExceptionNotificationListener;
import org.mule.runtime.api.notification.NotificationListenerRegistry;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;

import com.github.tomakehurst.wiremock.client.WireMock;
import io.qameta.allure.Issue;
import org.junit.Before;
import org.junit.Test;

public class OAuthSourceRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  private static List<CoreEvent> EVENTS;
  private static final int PROBE_TIMEOUT = 5000;
  private static final int PROBE_FREQUENCY = 500;

  @Inject
  protected NotificationListenerRegistry notificationListenerRegistry;

  private Flow flow;


  public static class Collector implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      synchronized (EVENTS) {
        EVENTS.add(event);
        return event;
      }
    }
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"auth-code-oauth-extension-static-config.xml", "sources-oauth-extension-flows.xml"};
  }

  @Before
  public void clearEvents() {
    EVENTS = new ArrayList<>();
  }


  @Before
  public void setOwnerId() throws Exception {
    ownerId = getCustomOwnerId();
  }

  @Override
  protected void doTearDown() throws Exception {
    if (flow != null) {
      flow.stop();
    }

    super.doTearDown();
  }

  @Test
  public void refreshTokenOnPollingSource() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    startFlow("pollingSource");

    check(PROBE_TIMEOUT, PROBE_FREQUENCY, () -> EVENTS.size() > 0);
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  public void refreshTokenOnSource() throws Exception {
    simulateCallback();

    WireMock.reset();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    startFlow("source");

    check(PROBE_TIMEOUT, PROBE_FREQUENCY, () -> EVENTS.size() > 0);
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  @Test
  @Issue("W-13628406")
  public void refreshTokenFailsOnSource() throws Exception {
    assertRefreshTokenOnSource("source");
  }

  @Test
  @Issue("W-13628406")
  public void refreshTokenFailsOnPollingSource() throws Exception {
    assertRefreshTokenOnSource("pollingSource");
  }

  private void assertRefreshTokenOnSource(String flowName) throws Exception {
    simulateCallback();

    WireMock.reset();
    wireMock.stubFor(post(urlMatching("/" + TOKEN_PATH)).willReturn(aResponse()
        .withStatus(INTERNAL_SERVER_ERROR.getStatusCode())
        .withBody("no token for you!")));

    Latch latch = new Latch();
    final ExceptionNotificationListener listener = notification -> {
      if (notification.getException().getMessage().contains("Refresh token workflow was attempted but failed")) {
        latch.release();
      }
    };

    notificationListenerRegistry.registerListener(listener);
    startFlow(flowName);
    latch.await(PROBE_TIMEOUT, MILLISECONDS);

    WireMock.reset();
    simulateCallback();
    stubTokenUrl(accessTokenContent(ACCESS_TOKEN + "-refreshed"));

    check(PROBE_TIMEOUT, PROBE_FREQUENCY, () -> EVENTS.size() > 0);
    wireMock.verify(postRequestedFor(urlPathEqualTo("/" + TOKEN_PATH)));
  }

  protected void startFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.start();
  }

}

