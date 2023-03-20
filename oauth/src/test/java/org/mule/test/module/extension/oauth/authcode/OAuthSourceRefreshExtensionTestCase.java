/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static org.mule.tck.probe.PollingProber.check;

import org.junit.Before;
import org.junit.Test;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.ArrayList;
import java.util.List;

import com.github.tomakehurst.wiremock.client.WireMock;

public class OAuthSourceRefreshExtensionTestCase extends BaseOAuthExtensionTestCase {

  private static List<CoreEvent> EVENTS;
  private static final int PROBE_TIMEOUT = 5000;
  private static final int PROBE_FREQUENCY = 500;

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

  protected void startFlow(String flowName) throws Exception {
    flow = (Flow) getFlowConstruct(flowName);
    flow.start();
  }

}

