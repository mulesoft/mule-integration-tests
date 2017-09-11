/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.functional.api;

import static org.mule.runtime.core.api.util.StringUtils.EMPTY;
import org.mule.runtime.api.exception.MuleException;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.junit.Test;

public class OnErrorAssertTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "error-assert.xml";
  }

  @Test
  public void emptyExceptionLog() throws Exception {
    flowRunner("testEmptyExceptionLog").run();
  }

  @Test
  public void noStacktraceException() throws Exception {
    flowRunner("noStacktraceException").run();
  }

  @Test
  public void stacktraceException() throws Exception {
    flowRunner("stacktraceException").run();
  }

  public static class EmptyException extends MuleException {

    @Override
    public String getDetailedMessage() {
      return EMPTY;
    }
  }

  public static class NoStacktraceException extends MuleException {

    @Override
    public String getDetailedMessage() {
      return "This is the logged exception\nexpected to be checked";
    }

  }

  public static class StacktraceException extends MuleException {

    @Override
    public String getDetailedMessage() {
      StringWriter w = new StringWriter();
      PrintWriter p = new PrintWriter(w);
      new Exception().printStackTrace(p);
      return w.toString();
    }

  }

}
