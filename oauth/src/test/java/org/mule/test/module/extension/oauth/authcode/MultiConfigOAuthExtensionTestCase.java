/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

public class MultiConfigOAuthExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public DynamicPort otherCallbackPort = new DynamicPort("otherCallbackPort");

  @Override
  protected String getConfigFile() {
    return "multi-config-owner-oauth-extension-config.xml";
  }

  @Before
  public void setOwnerId() throws Exception {
    ownerId = DEFAULT_OWNER_ID;
  }

  @Test
  public void authorizeAndStartDancingBaby() throws Exception {
    startDance(callbackPort.getNumber());

    simulateDanceStart(otherCallbackPort.getNumber());
    verifyAuthUrlRequest(otherCallbackPort.getNumber());
  }

  @Test
  public void receiveAccessTokenAndUserConnection() throws Exception {
    simulateCallback();

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner("getConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);
    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, ownerId);

    getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY).clear();
    simulateCallback(otherCallbackPort.getNumber());

    TestOAuthConnectionState otherConnection = ((TestOAuthConnection) flowRunner("getOtherConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(otherConnection);
    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, ownerId);
  }

  @Test
  public void unauthorize() throws Exception {
    startDance(callbackPort.getNumber());
    getConnection(callbackPort.getNumber(), "getConnection");

    flowRunner("unauthorize").run();
    ObjectStore objectStore = getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY);
    assertThat(objectStore.retrieveAll().size(), is(0));

    startDance(otherCallbackPort.getNumber());
    getConnection(otherCallbackPort.getNumber(), "getOtherConnection");

    flowRunner("unauthorizeOther").run();
    assertThat(objectStore.retrieveAll().size(), is(0));
  }

  private void getConnection(int port, String flowName) throws Exception {
    simulateCallback(port);

    TestOAuthConnectionState connection = ((TestOAuthConnection) flowRunner(flowName)
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connection);
    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, ownerId);
  }


  private void startDance(int port) throws IOException {
    simulateDanceStart(port);
    verifyAuthUrlRequest(port);
  }
}
