/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import org.mule.runtime.core.privileged.profiling.CapturedExportedSpan;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.bootstrap.ArtifactType.APP;

/**
 * Utils class for tracing testing.
 *
 * TODO: this will be probably removed once W-11573985: Make tracing tests more declarative is done
 */
public class TracingTestUtils {

  private static final String ARTIFACT_TYPE_KEY = "artifactType";
  private static final String ARTIFACT_ID_KEY = "artifactId";
  private static final String THREAD_START_ID_KEY = "threadStartId";
  private static final String LOCATION_KEY = "location";
  private static final String CORRELATION_ID_KEY = "correlationId";

  public static void assertSpanAttributes(CapturedExportedSpan span, String location,
                                          String artifactId) {
    assertThat(span.getAttributes().get(CORRELATION_ID_KEY), notNullValue());
    assertThat(span.getAttributes().get(ARTIFACT_TYPE_KEY), equalTo(APP.getAsString()));
    assertThat(span.getAttributes().get(ARTIFACT_ID_KEY), equalTo(artifactId));
    assertThat(span.getAttributes().get(THREAD_START_ID_KEY), notNullValue());
    assertThat(span.getAttributes().get(LOCATION_KEY), equalTo(location));
    assertThat(span.getServiceName(), equalTo(artifactId));
  }

}
