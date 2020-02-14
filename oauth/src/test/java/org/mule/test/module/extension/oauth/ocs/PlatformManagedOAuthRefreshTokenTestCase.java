/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.test.oauth.TestOAuthConnection;

import org.hamcrest.MatcherAssert;
import org.junit.Test;

public class PlatformManagedOAuthRefreshTokenTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final int TIMES_CALLED_FLAKY_OPERATION = 10;

  @Test
  public void accessTokenRetrieval() throws Exception {
    TestOAuthConnection connection = null;

    for (int i = 0; i < TIMES_CALLED_FLAKY_OPERATION; i++) {
      connection = (TestOAuthConnection) flowRunner("getFlackyConnection").run().getMessage().getPayload().getValue();
    }
    MatcherAssert.assertThat(connection.getState().getState().getAccessToken(), is(TEST_ACCESS_TOKEN));
    verify(mockPlatformDancer, times(TIMES_CALLED_FLAKY_OPERATION)).refreshToken();
  }

}
