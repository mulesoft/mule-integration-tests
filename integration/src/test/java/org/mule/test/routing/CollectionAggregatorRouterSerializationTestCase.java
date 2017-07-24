/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.routing;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.core.api.client.MuleClient;
import org.mule.runtime.core.api.config.MuleProperties;
import org.mule.runtime.api.serialization.SerializationException;
import org.mule.runtime.api.store.ObjectStoreException;
import org.mule.runtime.core.internal.routing.EventGroup;
import org.mule.runtime.core.api.store.SimpleMemoryObjectStore;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.SerializationUtils;
import org.junit.Test;

public class CollectionAggregatorRouterSerializationTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "collection-aggregator-router-serialization.xml";
  }

  @Test
  public void eventGroupDeserialization() throws Exception {
    muleContext.getRegistry().registerObject(MuleProperties.OBJECT_STORE_DEFAULT_IN_MEMORY_NAME,
                                             new EventGroupSerializerObjectStore());
    List<String> list = Arrays.asList("first", "second");
    flowRunner("splitter").withPayload(list).run();

    MuleClient client = muleContext.getClient();
    Message request = client.request("test://out", RECEIVE_TIMEOUT).getRight().get();
    assertNotNull(request);
    assertThat(request.getPayload().getValue(), instanceOf(List.class));
    assertThat(((List<Message>) request.getPayload().getValue()), hasSize(list.size()));
  }

  private class EventGroupSerializerObjectStore extends SimpleMemoryObjectStore<Serializable> {

    @Override
    protected void doStore(String key, Serializable value) throws ObjectStoreException {
      if (value instanceof EventGroup) {
        value = SerializationUtils.serialize(value);
      }
      super.doStore(key, value);
    }

    @Override
    protected Serializable doRetrieve(String key) throws ObjectStoreException {
      Object value = super.doRetrieve(key);
      if (value instanceof byte[]) {
        try {
          value = SerializationUtils.deserialize((byte[]) value);
        } catch (SerializationException e) {
          // return original value
        }
      }
      return (Serializable) value;
    }
  }
}
