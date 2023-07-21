/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.streaming;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;

/**
 * A simple bridge service for testing entry point resolution
 */
public class SimpleStreamingBean {

  public byte[] doit(InputStream in) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    IOUtils.copy(in, out);
    in.close();
    return out.toByteArray();
  }
}
