/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.streaming;

import static java.lang.Math.min;
import static java.nio.charset.Charset.defaultCharset;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.STREAMING;

import java.io.File;
import java.io.InputStream;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(HTTP_EXTENSION)
@Story(STREAMING)
public class ManageLostClientConnectionsTestCase extends AbstractIntegrationTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port");

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  public HttpClient httpClientCustom;

  @Before
  public void setupClient() {
    HttpClientConfiguration config = new HttpClientConfiguration.Builder()
        .setName(this.getClass().getName())
        .setStreaming(true)
        .build();

    httpClientCustom = getService(HttpService.class)
        .getClientFactory()
        .create(config);

    httpClientCustom.start();
  }

  @Test
  public void testSyncResponse() throws Exception {
    Semaphore sem = new Semaphore(1);

    String fileContent = randomAlphanumeric(8192 * 300);
    File file = new File(temporaryFolder.getRoot(), "file.txt");
    writeStringToFile(file, fileContent, defaultCharset());

    String url = String.format("http://localhost:%d/getFile", dynamicPort.getNumber());

    HttpRequest request = HttpRequest.builder().uri(url)
        .entity(new ByteArrayHttpEntity("request".getBytes())).method(POST).build();

    // sem.acquire();
    // httpClientCustom.sendAsync(request, HttpRequestOptions.builder().build())
    // .whenCompleteAsync((response, ex) -> {
    // if (response != null) {
    // try {
    // long count = 0;
    // byte[] buffer = new byte[1024];
    // InputStream content = response.getEntity().getContent();
    // while (content.available() > 0) {
    // count += content.read(buffer, 0, min(buffer.length, content.available()));
    // httpClientCustom.stop();
    // }
    // System.out.println("\n");
    // System.out.println("Read: " + count + " bytes");
    // } catch (Exception e) {
    // System.out.println("Error reading response");
    // e.printStackTrace();
    // }
    // } else {
    // System.out.println("completed with error!");
    // ex.printStackTrace();
    // }
    // sem.release();
    // });

    CompletableFuture<HttpResponse> responseFuture = httpClientCustom.sendAsync(request, HttpRequestOptions.builder().build());
    HttpResponse response = responseFuture.get();

    if (response != null) {
      try {
        long count = 0;
        byte[] buffer = new byte[10240000];
        InputStream content = response.getEntity().getContent();
        while (content.available() > 0) {
          //count += content.read(buffer, 0, min(buffer.length, content.available()));
          //count += content.read(buffer, 0, buffer.length);
          TimeUnit.MILLISECONDS.sleep(1000);
          // httpClientCustom.stop();
        }
        System.out.println("\n");
        System.out.println("Read: " + count + " bytes");
      } catch (Exception e) {
        System.out.println("Error reading response");
        e.printStackTrace();
      }
    } else {
      System.out.println("completed with error!");
    }
    // sem.release();

    // System.out.println("Warten auf dich");
    // try {
    // sem.acquire();
    // } catch (InterruptedException e) {
    // e.printStackTrace();
    // }
    System.out.println("Das Ende!");
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/managed-lost-client-connections-config.xml";
  }
}

