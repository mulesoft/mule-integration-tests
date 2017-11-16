/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package extension.org.mule.soap.it;

import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import org.mule.runtime.api.connection.ConnectionException;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.client.ExtensionsClient;
import org.mule.runtime.extension.api.soap.MessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;
import org.mule.runtime.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TestHttpDispatcherProvider implements MessageDispatcherProvider<MessageDispatcher>, Lifecycle {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestHttpDispatcherProvider.class);

  @Inject
  private HttpService httpService;

  @Inject
  private ExtensionsClient client;

  private HttpClient httpClient;

  @Parameter
  @Optional
  private String requesterConfig;

  @Override
  public MessageDispatcher connect() throws ConnectionException {
    return requesterConfig == null ?
             new DefaultHttpMessageDispatcher(httpClient) :
             new HttpConfigBasedMessageDispatcher(requesterConfig, client);
  }

  @Override
  public void disconnect(MessageDispatcher connection) {
    disposeIfNeeded(connection, LOGGER);
  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection) {
    return success();
  }

  @Override
  public void dispose() {
    // Do nothing
  }

  @Override
  public void initialise() throws InitialisationException {
    httpClient = httpService.getClientFactory().create(new HttpClientConfiguration.Builder()
                                                         .setName("workday")
                                                         .build());
  }

  @Override
  public void stop() throws MuleException {
    httpClient.stop();
  }

  @Override
  public void start() throws MuleException {
    httpClient.start();
  }
}
