/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.http.api.HttpService;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CORRELATION_ID;
import static org.mule.test.allure.AllureConstants.CorrelationIdFeature.CorrelationIdOnSourcesStory.CORRELATION_ID_ON_SOURCES;

@Issue("MULE-18770")
@Feature(CORRELATION_ID)
@Story(CORRELATION_ID_ON_SOURCES)
public class SourceCorrelationIdSourceGeneratedTestCase extends AbstractIntegrationTestCase {

  private static final String TEST_CORRELATION_ID = "cheems";

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/correlation-id/source-generated.xml";
  }

  @Test
  public void execute() throws Exception {
    CoreEvent result = flowRunner("execute").withSourceCorrelationId(TEST_CORRELATION_ID).run();
    String correlationID = result.getMessage().getPayload().getValue().toString();
    assertThat(correlationID, is(TEST_CORRELATION_ID));
  }

}
