/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static java.lang.String.format;
import static java.util.Arrays.asList;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
@RunnerDelegateTo(Parameterized.class)
public class PayloadStatisticsTestCase extends AbstractIntegrationTestCase {

  public static final int BYTES_SIZE = 1343;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Rule
  public SystemProperty bytesSize = new SystemProperty("bytesSize", "" + BYTES_SIZE);

  @Rule
  public SystemProperty withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Rule
  public DynamicPort port = new DynamicPort("port");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  private TestConnectorQueueHandler queueHandler;

  @Inject
  @Named("listOfMessagesSource")
  public Flow listOfMessagesSource;

  private final String configFile;

  @Parameters(name = "{0}")
  public static Collection<String> data() {
    return asList("payload-statistics-config.xml",
                  "payload-statistics-non-repeatable-config.xml");
  }

  public PayloadStatisticsTestCase(String configFile) {
    this.configFile = configFile;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/" + configFile;
  }

  @Before
  public void before() throws IOException {
    cleanDirectory(temporaryFolder.getRoot());
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  @Description("Assert statistics for a source that generates a List of objects with an iterator")
  public void bytesSource() throws MuleException, IOException, TimeoutException {
    HttpRequest httpRequest = HttpRequest.builder()
        .method(POST)
        .uri(format("http://localhost:%d", port.getNumber()))
        .entity(new ByteArrayHttpEntity(randomAlphanumeric(BYTES_SIZE).getBytes()))
        .build();

    HttpRequestOptions options = HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, options);

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("bytesSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("http:listener"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 1L));
  }

  @Test
  @Issue("MULE-18894")
  @Description("Assert statistics for a source that generates a List of objects with an iterator through VM with serialization")
  public void bytesSourceThroughVM() throws MuleException, IOException, TimeoutException {
    HttpRequest httpRequest = HttpRequest.builder()
        .method(POST)
        .uri(format("http://localhost:%d/throughVM", port.getNumber()))
        .entity(new ByteArrayHttpEntity(randomAlphanumeric(BYTES_SIZE).getBytes()))
        .build();

    HttpRequestOptions options = HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).build();

    HttpResponse httpResponse = httpClient.send(httpRequest, options);

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("bytesSourceThroughVM/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("http:listener"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 1L));
  }

  @Test
  @Description("Assert statistics for a source that generates a List of objects with an iterator")
  public void listOfMessagesSource() throws MuleException {
    listOfMessagesSource.start();

    queueHandler.read("listOfMessagesSourceComplete", RECEIVE_TIMEOUT).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("listOfMessagesSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:cerebro-detect-new-mutants"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    // do not count the container message
    assertThat(fileListStatistics.getOutputObjectCount(), is(3L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));

  }

  @Test
  @Description("Assert statistics for an operation that generates a List of objects with a stream")
  public void listOfMessagesOperation() throws Exception {
    flowRunner("listOfMessagesOperation").run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("listOfMessagesOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:adamantium-injectors"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    // do not count the container message
    assertThat(fileListStatistics.getOutputObjectCount(), is(4L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 4L));

  }

  @Test
  @Description("Assert statistics for an operation that returns a PagingProvider of objects with a stream")
  public void pagesOfMessagesOperation() throws Exception {
    for (int i = 0; i < 3; ++i) {
      writeStringToFile(new File(temporaryFolder.getRoot(), "file_" + i + ".txt"), randomAlphanumeric(BYTES_SIZE));
    }

    flowRunner("pagesOfMessagesOperation").withVariable("path", temporaryFolder.getRoot().getPath()).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("pagesOfMessagesOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("file:list"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(3L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 3L));
  }

  @Test
  @Description("Assert statistics for an operation that returns a PagingProvider")
  public void pagedOperation() throws Exception {
    flowRunner("pagedOperation").run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("pagedOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:get-relics"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(9L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

  @Test
  @Description("Assert statistics for an operation that returns an InputStream")
  public void streamOperation() throws Exception {
    flowRunner("streamOperation").withPayload(randomAlphanumeric(BYTES_SIZE)).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("streamOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:to-stream"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 1L));
  }

  @Test
  @Description("Assert statistics for an operation that returns an Iterator")
  public void iteratorOperation() throws Exception {
    flowRunner("iteratorOperation").run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("iteratorOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:wolverine-blacklist"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(6L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

}
