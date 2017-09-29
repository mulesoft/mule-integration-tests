/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import org.mule.runtime.core.api.event.CoreEvent;

import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

public class TransformationContentTypeTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "content-type-setting-transformer-configs.xml";
  }

  @Test
  public void testReturnType() throws Exception {
    String inputMessage = "ABCDEF";
    CoreEvent event = flowRunner("test").withPayload(inputMessage).run();
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getPrimaryType(), is("text"));
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getSubType(), is("plain"));
    assertThat(event.getMessage().getPayload().getDataType().getMediaType().getCharset().get(), is(ISO_8859_1));
  }

}
