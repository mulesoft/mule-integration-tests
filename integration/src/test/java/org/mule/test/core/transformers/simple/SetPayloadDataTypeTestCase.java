/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.UTF_16;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.nio.charset.Charset;

public class SetPayloadDataTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "set-payload-data-type-config.xml";
  }

  @Test
  public void setsPayloadLocal() throws Exception {
    assertPayloadMediaType("setPayload", MediaType.XML, UTF_16);
  }

  @Test
  public void setsPayloadLocalWithDefaultMediaType() throws Exception {
    assertPayloadMediaType("setPayloadWithDefaultMediaType", MediaType.ANY, null);
  }

  @Test
  public void setsPayloadLocalWithDW() throws Exception {
    assertPayloadMediaType("setPayloadWithDW", MediaType.XML, null);
  }

  @Test
  public void setsPayloadLocalWithDWsettingMediaType() throws Exception {
    Message response = getResponse("setPayloadWithDWsettingMediaType");

    final MediaType JSON_UTF8 = MediaType.create(JSON.getPrimaryType(), JSON.getSubType(), UTF_8);
    DataType dataType = response.getPayload().getDataType();
    assertThat(CursorStreamProvider.class.isAssignableFrom(dataType.getType()), is(true));
    assertThat(dataType.getMediaType(), is(JSON_UTF8));
  }

  private Message getResponse(String flowName) throws Exception {
    return flowRunner(flowName).withPayload(TEST_MESSAGE).run().getMessage();
  }

  private void assertPayloadMediaType(String flowName, MediaType expectedMediaType, Charset charset) throws Exception {
    Message response = getResponse(flowName);

    assertThat(response.getPayload().getDataType(), like(String.class, expectedMediaType, charset));
  }

}
