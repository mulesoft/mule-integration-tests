/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.listener;

import org.mule.test.runner.RunnerDelegateTo;

import org.junit.runners.Parameterized;

@RunnerDelegateTo(Parameterized.class)
public class HttpListenerExpectHeaderStreamingAutoStringTestCase extends HttpListenerExpectHeaderStreamingNeverTestCase {

  @Override
  protected String getConfigFile() {
    return "http-listener-expect-header-streaming-auto-string-config.xml";
  }

  public HttpListenerExpectHeaderStreamingAutoStringTestCase(String persistentConnections) {
    super(persistentConnections);
  }

}

