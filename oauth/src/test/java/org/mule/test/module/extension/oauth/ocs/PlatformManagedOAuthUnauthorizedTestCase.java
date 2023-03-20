/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.junit.Test;

public class PlatformManagedOAuthUnauthorizedTestCase extends PlatformManagedOAuthConfigurationTestCase {

  private static final int TIMES_CALLED_UNAUTHORIZE_OPERATION = 10;

  @Test
  public void contextIsInvalidatedOnUnauthorizeOperation() throws Exception {
    for (int i = 0; i < TIMES_CALLED_UNAUTHORIZE_OPERATION; i++) {
      flowRunner("unauthorizeConnection").run();
    }
    verify(mockPlatformDancer, times(TIMES_CALLED_UNAUTHORIZE_OPERATION)).invalidateContext();
  }

}
