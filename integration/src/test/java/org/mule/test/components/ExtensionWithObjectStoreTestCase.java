/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ObjectStoreFeature.OS_EXTENSION;
import static org.mule.test.allure.AllureConstants.ObjectStoreFeature.ObjectStoreStory.OBJECT_STORE_AS_OPERATION_PARAMETER;

import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(OS_EXTENSION)
@Story(OBJECT_STORE_AS_OPERATION_PARAMETER)
public class ExtensionWithObjectStoreTestCase extends AbstractIntegrationTestCase {

  @Inject
  private ObjectStoreManager objectStoreManager;

  @Inject
  private org.mule.sdk.api.store.ObjectStoreManager sdkObjectStoreManager;

  @Override
  protected String getConfigFile() {
    return "extension-with-objectstore-config.xml";
  }

  @Test
  @Description("Operation has a parameter which points to a globally defined object store")
  public void storeOnGlobalStore() throws Exception {
    assertStoreValue("storeMoneyOnGlobalStore", "bank", "money", 1234L);
  }

  @Test
  @Description("Operation has a parameter which points to a private ObjectStore defined inline")
  public void storeOnPrivateStore() throws Exception {
    assertStoreValue("storeMoneyOnPrivateStore", "burriedBarrel", "money", 1234L);
  }

  @Test
  @Description("Operation uses the Mule api Object Store Manager which is injected in the extension via @Inject annotation")
  public void storeUsingMuleObjectStoreManager() throws Exception {
    assertStoreValue("storeUsingMuleObjectStoreManager", "extensionObjectStore", "mule-money", 1500L);
  }

  @Test
  @Description("Operation uses the Sdk api Object Store Manager which is injected in the extension via @Inject annotation")
  public void storeUsingSdkObjectStoreManager() throws Exception {
    assertStoreValue("storeUsingSdkObjectStoreManager", "extensionObjectStore", "sdk-money", 2500L);
  }

  private void assertStoreValue(String flowName, String osName, String key, Long value) throws Exception {
    flowRunner(flowName).run();

    ObjectStore<Long> objectStore = objectStoreManager.getObjectStore(osName);
    assertThat(objectStore.retrieve(key), equalTo(value));
  }
}
