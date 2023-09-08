/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.usecases.routing.response;

import static org.mule.runtime.api.store.ObjectStoreManager.BASE_IN_MEMORY_OBJECT_STORE_KEY;
import static org.mule.runtime.http.api.HttpConstants.Method.POST;
import static org.mule.runtime.http.api.client.HttpRequestOptions.builder;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.api.store.SimpleMemoryObjectStore;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.api.util.IOUtils;
import org.mule.runtime.http.api.HttpService;
import org.mule.runtime.http.api.domain.entity.ByteArrayHttpEntity;
import org.mule.runtime.http.api.domain.message.request.HttpRequest;
import org.mule.runtime.http.api.domain.message.response.HttpResponse;
import org.mule.service.http.TestHttpClient;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

public class SerializationOnResponseAggregatorTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort dynamicPort = new DynamicPort("port1");

  @Rule
  public TestHttpClient httpClient = new TestHttpClient.Builder(getService(HttpService.class)).build();

  @Override
  protected String getConfigFile() {
    return "org/mule/test/usecases/routing/response/serialization-on-response-router-config.xml";
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    builders.add(new AbstractConfigurationBuilder() {

      @Override
      public void doConfigure(MuleContext muleContext) throws ConfigurationException {
        muleContext.getCustomizationService().overrideDefaultServiceImpl(BASE_IN_MEMORY_OBJECT_STORE_KEY, new TestObjectStore());
      }
    });

    super.addBuilders(builders);
  }

  @Test
  public void testSyncResponse() throws Exception {
    HttpRequest request = HttpRequest.builder().uri("http://localhost:" + dynamicPort.getNumber())
        .entity(new ByteArrayHttpEntity("request".getBytes())).method(POST).build();

    HttpResponse response = httpClient.send(request, builder().responseTimeout(RECEIVE_TIMEOUT)
        .followsRedirect(false)
        .build());

    String payload = IOUtils.toString(response.getEntity().getContent());
    assertThat(payload, is("request processed"));
  }

  private static class TestObjectStore extends SimpleMemoryObjectStore<Serializable> {

    @Inject
    private ObjectSerializer serializer;

    @Override
    protected void doStore(String key, Serializable value) throws ObjectStoreException {
      byte[] serialized = serializer.getExternalProtocol().serialize(value);
      super.doStore(key, serialized);
    }

    @Override
    protected Serializable doRetrieve(String key) throws ObjectStoreException {
      Serializable serialized = super.doRetrieve(key);
      return serializer.getExternalProtocol().deserialize((byte[]) serialized);
    }
  }
}
