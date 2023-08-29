/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.listener;


import org.mule.tck.junit4.AbstractMuleContextTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class HttpListenerExpectHeaderStreamingAlwaysTestCase extends HttpListenerExpectHeaderStreamingNeverTestCase {

  @Override
  protected String getConfigFile() {
    return "http-listener-expect-header-streaming-always-config.xml";
  }

  public HttpListenerExpectHeaderStreamingAlwaysTestCase(String persistentConnections) {
    super(persistentConnections);
  }

  @Override
  protected String getExpectedResponseBody() {
    return "c\r\n" + AbstractMuleContextTestCase.TEST_MESSAGE + "\r\n0\r\n\r";
  }
}

