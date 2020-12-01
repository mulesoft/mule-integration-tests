/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.mule.runtime.api.store.ObjectStoreSettings.unmanagedTransient;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.tck.junit4.FlakinessDetectorTestRunner;
import org.mule.tck.junit4.FlakyTest;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import org.junit.Test;
import org.mule.test.runner.RunnerDelegateTo;

@RunnerDelegateTo(FlakinessDetectorTestRunner.class)
public class CustomStoreOAuthExtensionTestCase extends BaseOAuthExtensionTestCase {


  private ObjectStore objectStore;

  @Override
  protected String getConfigFile() {
    return "custom-store-oauth-extension-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    ownerId = getCustomOwnerId();
    storedOwnerId = getCustomOwnerId() + "-oauth";
    objectStore = muleContext.getObjectStoreManager().createObjectStore(CUSTOM_STORE_NAME, unmanagedTransient());
  }

  @Override
  protected void doTearDown() throws Exception {
    muleContext.getObjectStoreManager().disposeStore(CUSTOM_STORE_NAME);
  }

  @Test
  @FlakyTest(times = 500)
  public void useCustomStore() throws Exception {
    simulateDanceStart();
    simulateCallback();

    assertOAuthStateStored(CUSTOM_STORE_NAME, storedOwnerId, ownerId);
  }
}
