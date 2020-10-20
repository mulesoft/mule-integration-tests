/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.module.tooling;

import static java.nio.charset.Charset.defaultCharset;
import static java.util.Optional.of;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.util.Pair;

import java.util.Optional;

import org.junit.Test;

public class DeclarationSessionWithResourcesTestCase extends DeclarationSessionTestCase {

  private static final String SOME_SECRET = "some secret";

  @Override
  protected Optional<Pair<String, byte[]>> getResource() {
    // Just use default encoding as connector is going to compare Strings using defaultCharset too.
    return of(new Pair("connection/secrets/keyStore.jks", SOME_SECRET.getBytes(defaultCharset())));
  }

  @Override
  protected Optional<String> getExpectedResourceContent() {
    return of(SOME_SECRET);
  }

  @Test
  public void resourcesAreVisibleToPlugin() {
    ConnectionValidationResult connectionValidationResult = session.testConnection(CONFIG_NAME);
    assertThat(connectionValidationResult.isValid(), is(true));
  }
}
