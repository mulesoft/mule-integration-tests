/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.security;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.extension.http.api.HttpSystemProperties.refresh;
import static org.mule.extension.http.internal.HttpConnectorConstants.BASIC_LAX_DECODING_PROPERTY;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;

import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;

public class HttpCompatibilityAuthenticationTestCase extends HttpListenerAuthenticationTestCase {

  @Rule
  public SystemProperty laxHeader = new SystemProperty(BASIC_LAX_DECODING_PROPERTY, "true");

  @Before
  public void set() {
    refresh();
  }

  @AfterClass
  public static void reset() {
    refresh();
  }

  @Override
  protected void assertResultForExtendedHeader() {
    assertThat(httpResponse.getStatusLine().getStatusCode(), is(OK.getStatusCode()));
  }
}
