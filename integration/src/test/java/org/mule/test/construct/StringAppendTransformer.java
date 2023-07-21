/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.construct;

import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;

import java.nio.charset.Charset;
import java.util.List;

public class StringAppendTransformer extends AbstractTransformer {

  public static final String DEFAULT_TEXT = " transformed";

  private String message = "";

  public StringAppendTransformer() {}

  public StringAppendTransformer(String message) {
    this.message = message;
  }

  @Override
  protected Object doTransform(Object src, Charset enc) throws TransformerException {
    return src + message;
  }

  public void setMessage(String value) {
    this.message = value;
  }

  public static String appendDefault(String msg) {
    return append(DEFAULT_TEXT, msg);
  }

  public static String append(String append, String msg) {
    return msg + append;
  }

  /**
   * Tranforms a list of string to its concatenation
   *
   * @param args arguments to concatenate
   * @return arguments transformed as concatenation of string
   * @throws TransformerException
   */
  public Object transformArray(List<String> args) throws TransformerException {
    StringBuffer buffer = new StringBuffer();

    for (String arg : args) {
      buffer.append(arg);
    }

    return transform(buffer.toString());
  }
}
