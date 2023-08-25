/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
