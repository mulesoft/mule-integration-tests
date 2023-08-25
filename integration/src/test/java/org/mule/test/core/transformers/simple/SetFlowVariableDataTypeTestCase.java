/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.JSON;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class SetFlowVariableDataTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "set-flow-variable-data-type-config.xml";
  }

  @Test
  public void setsPropertyDataType() throws Exception {
    final CoreEvent event = flowRunner("setVariableFlow").withPayload(TEST_MESSAGE).run();
    DataType dataType1 = event.getVariables().get("var1").getDataType();
    DataType dataType2 = event.getVariables().get("var2").getDataType();
    DataType dataType3 = event.getVariables().get("var3").getDataType();
    DataType dataType4 = event.getVariables().get("var4").getDataType();

    assertThat(dataType1, like(String.class, XML, UTF_8));
    assertThat(dataType2, like(String.class, ANY, UTF_8));
    assertThat(dataType3, like(String.class, XML, UTF_8));

    final MediaType JSON_UTF8 = MediaType.create(JSON.getPrimaryType(), JSON.getSubType(), UTF_8);
    assertThat(CursorStreamProvider.class.isAssignableFrom(dataType4.getType()), is(true));
    assertThat(dataType4.getMediaType(), is(JSON_UTF8));
  }

}
