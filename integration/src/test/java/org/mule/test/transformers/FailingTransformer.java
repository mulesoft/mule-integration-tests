/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.transformers;

import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.runtime.core.api.transformer.AbstractTransformer;

import java.nio.charset.Charset;

public class FailingTransformer extends AbstractTransformer {

  @Override
  protected Object doTransform(Object src, Charset encoding) throws TransformerException {
    throw new TransformerException(this, new Exception("Wrapped test exception"));
  }

}
