/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package extension.org.mule.soap.it;

import static org.mule.runtime.api.connection.ConnectionValidationResult.failure;
import static org.mule.runtime.api.connection.ConnectionValidationResult.success;
import static org.mule.runtime.core.api.lifecycle.LifecycleUtils.disposeIfNeeded;

import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.lifecycle.Lifecycle;
import org.mule.runtime.extension.api.annotation.Alias;
import org.mule.runtime.extension.api.annotation.param.Optional;
import org.mule.runtime.extension.api.annotation.param.Parameter;
import org.mule.runtime.extension.api.soap.ContextAwareMessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.DispatchingContext;
import org.mule.runtime.extension.api.soap.SoapServiceProvider;
import org.mule.runtime.extension.api.soap.message.MessageDispatcher;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.client.HttpClient;
import org.mule.runtime.http.api.client.HttpClientConfiguration;
import org.mule.runtime.soap.api.message.dispatcher.DefaultHttpMessageDispatcher;
import org.mule.runtime.soap.api.message.dispatcher.HttpConfigBasedMessageDispatcher;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Alias("http-extensions-client-provider")
public class TestExtensionsClientHttpDispatcherProvider extends ContextAwareMessageDispatcherProvider<MessageDispatcher> {

  private static final Logger LOGGER = LoggerFactory.getLogger(TestExtensionsClientHttpDispatcherProvider.class);
  private static final String INVALID_REQUESTER_NAME = "invalid";

  @Parameter
  private String requesterConfig;

  @Override
  public MessageDispatcher connect(DispatchingContext ctx) {
    return new HttpConfigBasedMessageDispatcher(requesterConfig, ctx.getExtensionsClient());
  }

  @Override
  public void disconnect(MessageDispatcher connection) {
    disposeIfNeeded(connection, LOGGER);
  }

  @Override
  public ConnectionValidationResult validate(MessageDispatcher connection, SoapServiceProvider provider) {
    if (INVALID_REQUESTER_NAME.equals(requesterConfig)) {
      return failure("invalid requester name", new Exception());
    }
    if (provider instanceof TestServiceProvider) {
      if (((TestServiceProvider) provider).getPort().equals("invalidPort")) {
        return failure("invalid port name", new Exception());
      }
    }
    return success();
  }
}
