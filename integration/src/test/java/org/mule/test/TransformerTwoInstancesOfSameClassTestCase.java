/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;

import org.junit.Test;

public class TransformerTwoInstancesOfSameClassTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/transformers/transformer-two-instances-same-class.xml";
  }

  @Test
  public void differentValues() throws Exception {
    final CustomTransformer appendStringA = (CustomTransformer) registry.lookupByName("appendStringA").get();
    final CustomTransformer appendStringB = (CustomTransformer) registry.lookupByName("appendStringB").get();

    assertThat(appendStringA.getMessage(), is("A"));
    assertThat(appendStringB.getMessage(), is("B"));
  }

  public static class CustomTransformer extends AbstractTransformer {

    private String message;

    @Override
    protected Object doTransform(Object src, Charset enc) throws TransformerException {
      return src;
    }

    public void setMessage(String message) {
      this.message = message;
    }

    public String getMessage() {
      return this.message;
    }
  }
}
