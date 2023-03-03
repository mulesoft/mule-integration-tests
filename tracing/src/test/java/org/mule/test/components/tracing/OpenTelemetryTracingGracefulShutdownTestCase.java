/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components.tracing;

public class OpenTelemetryTracingGracefulShutdownTestCase extends AbstractOpenTelemetryTracingTestCase {

  public OpenTelemetryTracingGracefulShutdownTestCase(String exporterType, String schema, int port, String path, boolean secure) {
    super(exporterType, schema, port, path, secure);
  }
}
