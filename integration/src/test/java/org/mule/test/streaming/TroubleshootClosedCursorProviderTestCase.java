/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.streaming;

import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;

import static java.lang.String.format;
import static java.nio.charset.Charset.defaultCharset;
import static java.nio.charset.StandardCharsets.UTF_8;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.isA;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.expression.ExpressionRuntimeException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.TimeoutException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
@Ignore("TD-0147155")
public class TroubleshootClosedCursorProviderTestCase extends AbstractIntegrationTestCase {

  private static final String FILE_NAME = "dummy.txt";

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Rule
  public SystemProperty fileNameProp = new SystemProperty("fileName", FILE_NAME);

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/troubleshoot-cursor-provider-config.xml";
  }

  @Test
  public void trackCursorClosedOnOperation() throws Exception {
    expectedException.expect(MuleException.class);
    expectedException.expectCause(isA(ExpressionRuntimeException.class));
    expectedException
        .expectMessage(containsString("The cursor provider was open by closeStreamOnOperationFlow/processors/0/processors/0."));

    writeStringToFile(new File(temporaryFolder.getRoot(), FILE_NAME), "Hello", defaultCharset());

    flowRunner("closeStreamOnOperationFlow").run();
  }

  @Test
  public void trackCursorClosedOnSource() throws IOException, TimeoutException {
    HttpRequest httpRequest = HttpRequest.builder()
        .method(POST)
        .uri(format("http://localhost:%d/api/echo", port.getNumber()))
        .entity(new ByteArrayHttpEntity("Hello".getBytes()))
        .build();

    HttpRequestOptions options = HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, options);

    String payload =
        org.apache.commons.io.IOUtils.toString(httpResponse.getEntity().getContent(), UTF_8);

    assertThat(payload, containsString("org.mule.runtime.core.internal.streaming.CursorProviderAlreadyClosedException"));
    assertThat(payload, containsString("The cursor provider was open by closeStreamOnSourceFlow/source"));
  }

  public static class ClosePayloadProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      Object mcp = event.getMessage().getPayload().getValue();
      try {
        Method close = mcp.getClass().getMethod("close");
        assertNotNull("Expected a closeable payload", close);
        close.invoke(mcp);
      } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
        fail(e.getMessage());
      }
      return event;
    }
  }

}
