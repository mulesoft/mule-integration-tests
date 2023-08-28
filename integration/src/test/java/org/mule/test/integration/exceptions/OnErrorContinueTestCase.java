/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.exceptions;

import static org.mule.functional.junit4.matchers.MessageMatchers.hasPayload;
import static org.mule.functional.junit4.matchers.ThrowableCauseMatcher.hasCause;
import static org.mule.runtime.api.i18n.I18nMessageFactory.createStaticMessage;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.INTERNAL_SERVER_ERROR;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTP;
import static org.mule.runtime.http.api.HttpConstants.Protocol.HTTPS;
import static org.mule.test.allure.AllureConstants.ErrorHandlingFeature.ERROR_HANDLING;

import static java.lang.String.format;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MILLISECONDS;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.exception.MuleFatalException;
import org.mule.runtime.api.exception.MuleRuntimeException;
import org.mule.runtime.api.lifecycle.CreateException;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.api.tls.TlsContextFactory;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.retry.policy.RetryPolicyExhaustedException;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpConstants.Protocol;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpRequestOptions;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.entity.HttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringWriter;

import javax.inject.Inject;
import javax.jws.WebParam;
import javax.jws.WebResult;
import javax.jws.WebService;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(ERROR_HANDLING)
@Story("On Error Continue")
public class OnErrorContinueTestCase extends AbstractIntegrationTestCase {

  public static final String ERROR_PROCESSING_NEWS = "error processing news";
  public static final String JSON_RESPONSE =
      "{\"errorMessage\":\"error processing news\",\"userId\":15,\"title\":\"News title\"}";
  public static final String JSON_REQUEST = "{\"userId\":\"15\"}";

  @Inject
  private TestQueueManager queueManager;

  @Rule
  public DynamicPort dynamicPort1 = new DynamicPort("port1");
  @Rule
  public DynamicPort dynamicPort2 = new DynamicPort("port2");
  @Rule
  public DynamicPort dynamicPort3 = new DynamicPort("port3");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).tlsContextFactory(() -> {
    try {
      // Configure trust store in the client with the certificate of the server.
      return TlsContextFactory.builder().trustStorePath("ssltest-cacerts.jks").trustStorePassword("changeit").build();
    } catch (CreateException e) {
      throw new MuleRuntimeException(e);
    }
  }).build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/exceptions/on-error-continue-use-case-flow.xml";
  }

  @Test
  public void testHttpJsonErrorResponse() throws Exception {
    testJsonErrorResponse(getUrl(HTTP, dynamicPort1, "service"));
  }

  @Test
  public void testHttpsJsonErrorResponse() throws Exception {
    testJsonErrorResponse(getUrl(HTTPS, dynamicPort3, "httpsservice"));
  }

  @Test
  public void testJsonErrorResponse() throws Exception {
    assertResponse(flowRunner("continueProcessingActualMessage").withPayload(JSON_REQUEST).run().getMessage());
  }

  private void assertResponse(Message response) throws Exception {
    assertThat(response, is(notNullValue()));
    // compare the structure and values but not the attributes' order
    JsonNode actualJsonNode = new ObjectMapper().readTree(getPayloadAsString(response));
    JsonNode expectedJsonNode = new ObjectMapper().readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  private void testJsonErrorResponse(String endpointUri) throws Exception {
    HttpRequest request = HttpRequest.builder().uri(endpointUri).method(POST)
        .entity(new ByteArrayHttpEntity(JSON_REQUEST.getBytes())).build();

    final HttpEntity response = httpClient
        .send(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).followsRedirect(false).build()).getEntity();

    assertResponse(response);
  }

  private void assertResponse(HttpEntity responseEntity) throws Exception {
    assertThat(responseEntity, is(notNullValue()));
    // compare the structure and values but not the attributes' order
    JsonNode actualJsonNode =
        new ObjectMapper().readTree(IOUtils.toString(responseEntity.getContent()));
    JsonNode expectedJsonNode = new ObjectMapper().readTree(JSON_RESPONSE);
    assertThat(actualJsonNode, is(expectedJsonNode));
  }

  public static final String MESSAGE = "some message";
  public static final String MESSAGE_EXPECTED = "some message consumed successfully";

  @Test
  public void testCatchWithComponent() throws Exception {
    Message result = flowRunner("catchWithComponent").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " Caught"));
  }

  @Test
  public void testFullyDefinedCatchExceptionStrategyWithComponent() throws Exception {
    Message result =
        flowRunner("fullyDefinedCatchExceptionStrategyWithComponent").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " apt1 apt2 groovified"));
  }

  @Test
  public void onErrorTypeMatch() throws Exception {
    Message result = flowRunner("onErrorTypeMatch").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " apt1 apt2"));
  }

  @Test
  public void onErrorTypeMatchAny() throws Exception {
    Message result = flowRunner("onErrorTypeMatchAny").withPayload(MESSAGE).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is(MESSAGE + " apt1 apt2"));
  }

  @Test
  public void onErrorTypeMatchSeveral() throws Exception {
    Message result = flowRunner("onErrorTypeMatchSeveral").withPayload(true).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("true apt1 apt2"));

    result = flowRunner("onErrorTypeMatchSeveral").withPayload(false).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("false apt1 apt2"));
  }

  @Test
  public void onErrorTypeMatchNameWildcard() throws Exception {
    Message result = flowRunner("onErrorTypeMatchNameWildcard").withPayload(true).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("true apt1 apt2"));

    result = flowRunner("onErrorTypeMatchNameWildcard").withPayload(false).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(result, hasPayload(equalTo("false apt1 apt2")));
  }

  @Test
  public void onErrorTypeMatchNamespaceWildcard() throws Exception {
    Message result = flowRunner("onErrorTypeMatchNamespaceWildcard").withPayload(true).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(getPayloadAsString(result), is("true apt1 apt2"));

    result = flowRunner("onErrorTypeMatchNamespaceWildcard").withPayload(false).run().getMessage();
    assertThat(result, is(notNullValue()));
    assertThat(result, hasPayload(equalTo("false apt1 apt2")));
  }

  @Test
  public void onErrorContinueFailure() throws Exception {
    expectedException.expectCause(instanceOf(MuleFatalException.class));
    expectedException.expectCause(hasCause(instanceOf(NoClassDefFoundError.class)));
    flowRunner("failingHandler").run();
  }

  @Test
  public void doesNotHandleSourceErrors() throws Exception {
    HttpRequest request = HttpRequest.builder().uri(getUrl(HTTP, dynamicPort1, "sourceError")).method(POST)
        .entity(new ByteArrayHttpEntity(TEST_MESSAGE.getBytes())).build();
    final HttpResponse response =
        httpClient.send(request, HttpRequestOptions.builder().responseTimeout(RECEIVE_TIMEOUT).followsRedirect(false).build());

    assertThat(response.getStatusCode(), is(INTERNAL_SERVER_ERROR.getStatusCode()));
    assertThat(queueManager.read("out", RECEIVE_TIMEOUT, MILLISECONDS), is(nullValue()));
  }

  @Test
  public void handlesTryScope() throws Exception {
    flowRunner("withTry").run();
    assertThat(queueManager.read("out1", RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), hasPayload(equalTo("flow")));
    assertThat(queueManager.read("out2", RECEIVE_TIMEOUT, MILLISECONDS).getMessage(), hasPayload(equalTo("try")));
  }

  private String getUrl(Protocol protocol, DynamicPort port, String path) {
    return format("%s://localhost:%s/%s", protocol.getScheme(), port.getNumber(), path);
  }

  public static class FailingProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new RetryPolicyExhaustedException(createStaticMessage("Error."), mock(Initialisable.class));
    }

  }

  public static class ErrorProcessor implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      throw new NoClassDefFoundError("Test error");
    }

  }

  public static class LoadNewsProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      NewsRequest newsRequest;

      try {
        Object payload = event.getMessage().getPayload().getValue();
        if (payload instanceof CursorStreamProvider) {
          newsRequest = handleInputStream(((CursorStreamProvider) payload).openCursor());
        } else if (payload instanceof InputStream) {
          newsRequest = handleInputStream((InputStream) payload);
        } else if (payload instanceof String) {
          newsRequest = new ObjectMapper().readValue((String) payload, NewsRequest.class);
        } else {
          throw new RuntimeException("Cannot create an object from a " + payload.getClass().getName());
        }
      } catch (Exception e) {
        throw new RuntimeException(e);
      }

      NewsResponse newsResponse = new NewsResponse();
      newsResponse.setUserId(newsRequest.getUserId());
      newsResponse.setTitle("News title");
      return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(newsResponse).build()).build();
    }

    private NewsRequest handleInputStream(InputStream payload) throws IOException {
      NewsRequest newsRequest;
      InputStreamReader inputStreamReader = new InputStreamReader(payload, UTF_8);
      newsRequest = new ObjectMapper().readValue(inputStreamReader, NewsRequest.class);
      return newsRequest;
    }
  }

  public static class NewsErrorProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {

      NewsResponse newsResponse = (NewsResponse) event.getMessage().getPayload().getValue();
      newsResponse.setErrorMessage(ERROR_PROCESSING_NEWS);

      StringWriter writer = new StringWriter();
      try {
        new ObjectMapper().writeValue(writer, newsResponse);
      } catch (IOException e) {
        throw new DefaultMuleException(e);
      }

      return CoreEvent.builder(event).message(Message.builder(event.getMessage()).value(writer.toString()).build()).build();
    }
  }

  public static class NewsRequest {

    private int userId;

    public int getUserId() {
      return userId;
    }

    public void setUserId(int userId) {
      this.userId = userId;
    }
  }

  public static class NewsResponse {

    private int userId;
    private String title;
    private String errorMessage;

    public int getUserId() {
      return userId;
    }

    public void setUserId(int userId) {
      this.userId = userId;
    }

    public String getTitle() {
      return title;
    }

    public void setTitle(String title) {
      this.title = title;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
      this.errorMessage = errorMessage;
    }
  }

  @WebService
  public static class Echo {

    @WebResult(name = "text")
    public String echo(@WebParam(name = "text") String string) {
      throw new RuntimeException();
    }
  }

}
