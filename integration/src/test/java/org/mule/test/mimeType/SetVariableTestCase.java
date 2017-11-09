/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.mimeType;

import org.junit.Test;
import org.mule.runtime.api.metadata.DataType;
import org.mule.runtime.api.metadata.MediaType;
import org.mule.runtime.api.streaming.bytes.CursorStreamProvider;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.test.AbstractIntegrationTestCase;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mule.runtime.api.metadata.MediaType.ANY;
import static org.mule.runtime.api.metadata.MediaType.XML;
import static org.mule.tck.junit4.matcher.DataTypeMatcher.like;

public class SetVariableTestCase extends AbstractIntegrationTestCase {

  @Override
  public String getConfigFile() {
    return "org/mule/test/mimeType/set-variable-config.xml";
  }

  @Test
  public void correctMimeType() throws Exception {
    final CoreEvent event = flowRunner("main").withPayload(TEST_MESSAGE).run();
    DataType dataType1 = event.getVariables().get("var1").getDataType();
    DataType dataType2 = event.getVariables().get("var2").getDataType();
    DataType dataType3 = event.getVariables().get("var3").getDataType();
    DataType dataType4 = event.getVariables().get("var4").getDataType();

    assertThat(dataType1, like(String.class, XML, UTF_8));
    assertThat(dataType2, like(String.class, ANY, UTF_8));
    assertThat(dataType3, like(String.class, XML, UTF_8));

    final MediaType XML_UTF8 = MediaType.create(XML.getPrimaryType(), XML.getSubType(), UTF_8);
    assertThat(CursorStreamProvider.class.isAssignableFrom(dataType4.getType()), is(true));
    assertThat(dataType4.getMediaType(), is(XML_UTF8));
  }

}
