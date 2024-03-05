/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.client.deprecated;

import static org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext.DEFAULT_RESOURCE_OWNER_ID;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.oauth.internal.AbstractOAuthDancer;
import org.mule.runtime.oauth.internal.config.OAuthDancerConfig;

import java.util.concurrent.CompletableFuture;

/**
 * A dummy dancer that will actually do nothing.
 * <p>
 * We just want to call the refresh token logic from the base class to spot interoperability issues between classes from different
 * ClassLoaders.
 */
public class TestOAuthDancer extends AbstractOAuthDancer<OAuthDancerConfig> {

  protected TestOAuthDancer(OAuthDancerConfig config) {
    super(config);
  }

  @Override
  public void start() throws MuleException {
    super.start();
    doRefreshToken(() -> getContextForResourceOwner(DEFAULT_RESOURCE_OWNER_ID),
                   CompletableFuture::completedFuture);
  }
}
