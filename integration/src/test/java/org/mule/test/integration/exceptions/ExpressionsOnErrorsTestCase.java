/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ErrorHandlingStory.ERROR_HANDLER;

import static java.lang.System.lineSeparator;
import static java.nio.charset.StandardCharsets.UTF_8;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlMatching;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import com.github.tomakehurst.wiremock.junit.WireMockRule;

import org.apache.commons.io.IOUtils;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story(ERROR_HANDLER)
public class ExpressionsOnErrorsTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort wireMockPort = new DynamicPort("wireMockPort");

  @Rule
  public WireMockRule wireMock = new WireMockRule(wireMockConfig()
      .bindAddress("127.0.0.1")
      .port(wireMockPort.getNumber()));

  @Before
  public void setUp() {
    wireMock.stubFor(get(urlMatching("/500")).willReturn(aResponse().withStatus(500)));
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/expressions-on-errors.xml";
  }

  @Test
  public void detailedDescription() throws Exception {
    assertThat(flowRunner("detailedDescription").run().getMessage().getPayload().getValue(),
               is("An expected error occurred."));
  }

  @Test
  public void infoElement() throws Exception {
    assertThat(flowRunner("infoElement").run().getMessage().getPayload().getValue(),
               is("infoElement/processors/0 @ ExpressionsOnErrorsTestCase#infoElement:org/mule/test/integration/exceptions/expressions-on-errors.xml:18"));
  }

  @Test
  public void infoElementSdkOp() throws Exception {
    assertThat(flowRunner("infoElementSdkOp").run().getMessage().getPayload().getValue(),
               is("infoElementSdkOp/processors/0 @ ExpressionsOnErrorsTestCase#infoElementSdkOp:org/mule/test/integration/exceptions/expressions-on-errors.xml:27"));
  }

  @Test
  @Issue("MULE-18535")
  public void infoElementDeprecated() throws Exception {
    assertThat(flowRunner("infoElementDeprecated").run().getMessage().getPayload().getValue(),
               is("infoElementDeprecated/processors/0 @ ExpressionsOnErrorsTestCase#infoElementDeprecated:org/mule/test/integration/exceptions/expressions-on-errors.xml:36"));
  }

  @Test
  @Issue("MULE-18535")
  public void infoElementDeprecatedSdkOp() throws Exception {
    assertThat(flowRunner("infoElementDeprecatedSdkOp").run().getMessage().getPayload().getValue(),
               is("infoElementDeprecatedSdkOp/processors/0 @ ExpressionsOnErrorsTestCase#infoElementDeprecatedSdkOp:org/mule/test/integration/exceptions/expressions-on-errors.xml:47"));
  }

  @Test
  @Issue("MULE-18666")
  public void mesageToJson() throws Exception {
    final CursorStreamProvider value = (CursorStreamProvider) flowRunner("messageToJson")
        .keepStreamsOpen()
        .withPayload("Hello World!")
        .withAttributes("Adios Amigos")
        .run().getMessage().getPayload().getValue();
    final String result = IOUtils.toString(value.openCursor(), UTF_8);
    assertThat(result,
               containsString(lineSeparator() +
                   "  \"payload\": \"Hello World!\"," + lineSeparator()));
    assertThat(result,
               containsString(lineSeparator() +
                   "  \"attributes\": \"Adios Amigos\"" + lineSeparator()));
  }
}
