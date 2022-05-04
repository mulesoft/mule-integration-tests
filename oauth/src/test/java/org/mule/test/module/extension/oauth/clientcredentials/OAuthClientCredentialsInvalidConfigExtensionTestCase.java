/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.clientcredentials;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.junit.rules.ExpectedException.none;
import static org.mule.extension.http.api.HttpHeaders.Names.CONTENT_TYPE;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.OK;
import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import java.util.Map;

import org.hamcrest.CoreMatchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import org.mule.runtime.api.lifecycle.LifecycleException;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.util.LazyValue;
import org.mule.runtime.extension.api.connectivity.oauth.ClientCredentialsState;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.oauth.TestOAuthConnection;
import org.mule.test.oauth.TestOAuthConnectionState;

import com.github.tomakehurst.wiremock.client.WireMock;
import org.junit.rules.ExpectedException;

public class OAuthClientCredentialsInvalidConfigExtensionTestCase extends BaseOAuthExtensionTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"client-credentials-oauth-invalid-config-extension-config.xml"};
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    expectedException.expect(LifecycleException.class);
    expectedException.expectMessage("The uri provided must contain a host : no.scheme.url.com");
    super.doSetUpBeforeMuleContextCreation();
  }

  @Test
  public void tokenUrlWithNoScheme() throws Exception {
    fail();
  }

}
