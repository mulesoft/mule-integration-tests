/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import org.junit.Test;

public class ExtensionWithObjectStoreTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ObjectStoreManager objectStoreManager;

  @Override
  protected String getConfigFile() {
    return "extension-with-objectstore-config.xml";
  }

  @Test
  public void storeOnGlobalStore() throws Exception {
    assertStoreValue("storeMoneyOnGlobalStore", "bank");
  }

  @Test
  public void storeOnPrivateStore() throws Exception {
    assertStoreValue("storeMoneyOnPrivateStore", "burriedBarrel");
  }

  private void assertStoreValue(String flowName, String osName) throws Exception {
    flowRunner(flowName).run();

    ObjectStore<Long> objectStore = objectStoreManager.getObjectStore(osName);
    assertThat(objectStore.retrieve("money"), equalTo(1234L));
  }
}
