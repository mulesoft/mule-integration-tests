/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.mule.test.oauth.TestOAuthConnection;

import org.junit.Test;

public class PlatformManagedOAuthRefreshTokenTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final int TIMES_CALLED_FLAKY_OPERATION = 10;

  @Test
  public void accessTokenRetrieval() throws Exception {
    TestOAuthConnection connection = null;

    for (int i = 0; i < TIMES_CALLED_FLAKY_OPERATION; i++) {
      connection = (TestOAuthConnection) flowRunner("getFlackyConnection").run().getMessage().getPayload().getValue();
    }
    assertThat(connection.getState().getState().getAccessToken(), is(TEST_ACCESS_TOKEN));
    verify(mockPlatformDancer, times(TIMES_CALLED_FLAKY_OPERATION)).refreshToken();
  }

  @Test
  public void getStringAfterRefresh() throws Exception {
    String result = null;

    for (int i = 0; i < 1; i++) {
      result = (String) flowRunner("getStringAfterRefresh").run().getMessage().getPayload().getValue();
    }
    System.out.println("  **  /" + result + "/     ****");
    assertThat(result, is("hello"));
    verify(mockPlatformDancer, times(0)).refreshToken();
  }

}
