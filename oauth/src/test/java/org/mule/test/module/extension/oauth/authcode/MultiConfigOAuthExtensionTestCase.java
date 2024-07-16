/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.store.ObjectStoreManager.BASE_PERSISTENT_OBJECT_STORE_KEY;

import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import java.io.IOException;
import java.util.Map;

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
    validateObjectStoreEntries(1);

    getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY).clear();
    simulateCallback(otherCallbackPort.getNumber());

    TestOAuthConnectionState otherConnection = ((TestOAuthConnection) flowRunner("getOtherConnection")
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(otherConnection);
    assertOAuthStateStored(BASE_PERSISTENT_OBJECT_STORE_KEY, ownerId);
  }

  @Test
  public void unauthorize() throws Exception {
    executeFlow(callbackPort.getNumber(), "getConnection", "unauthorize", 1);
    executeFlow(otherCallbackPort.getNumber(), "getOtherConnection", "unauthorizeOther", 2);
  }

  private void executeFlow(int portNumber, String getConnectionFlow, String unauthorizeFlow, int expectedEntriesSize)
      throws Exception {
    startDance(portNumber);
    simulateCallback(portNumber);

    TestOAuthConnectionState connectionState = ((TestOAuthConnection) flowRunner(getConnectionFlow)
        .run().getMessage().getPayload().getValue()).getState();

    assertConnectionState(connectionState);

    validateObjectStoreEntries(expectedEntriesSize);

    flowRunner(unauthorizeFlow).run();
    validateObjectStoreEntries(expectedEntriesSize);
  }

  private void validateObjectStoreEntries(int expectedEntriesSize) throws Exception {
    Map<String, ResourceOwnerOAuthContext> entries = getObjectStore(BASE_PERSISTENT_OBJECT_STORE_KEY).retrieveAll();
    assertThat(entries.size(), is(expectedEntriesSize));
    ResourceOwnerOAuthContext context = (ResourceOwnerOAuthContext) entries.values().toArray()[0];
    assertThat(context.getResourceOwnerId(), is(ownerId));
  }

  private void startDance(int port) throws IOException {
    simulateDanceStart(port);
    verifyAuthUrlRequest(port);
  }
}
