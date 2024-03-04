/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.oauth.client.deprecated;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.api.scheduler.SchedulerService;
import org.mule.runtime.oauth.api.builder.ClientCredentialsLocation;
import org.mule.runtime.oauth.api.http.HttpClientFactory;
import org.mule.runtime.oauth.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.oauth.internal.builder.AbstractOAuthDancerBuilder;
import org.mule.runtime.oauth.internal.config.DefaultClientCredentialsOAuthDancerConfig;
import org.mule.runtime.oauth.internal.config.OAuthDancerConfig;

import java.util.Map;

/**
 * This OAuth Dancer Builder extends from an internal class: {@link AbstractOAuthDancerBuilder}.
 * <p>
 * This class is included as a compile-scoped dependency, otherwise we would not be able to use it in runtime, because the
 * Container will not export it (being internal).
 * <p>
 * Since we will be using a legacy {@link AbstractOAuthDancerBuilder}, it will also try to use legacy versions of API classes, for
 * example {@link ClientCredentialsLocation}. It needs to be able to find those also from the Extension's ClassLoader, because
 * they are no longer present in the Container after the split-package fix of W-13167592.
 */
public class TestOAuthDancerBuilder extends AbstractOAuthDancerBuilder<TestOAuthDancer> {

  public TestOAuthDancerBuilder(SchedulerService schedulerService, LockFactory lockProvider,
                                Map<String, ResourceOwnerOAuthContext> tokensStore,
                                HttpClientFactory baseHttpClientFactory,
                                MuleExpressionLanguage expressionEvaluator) {
    super(schedulerService, lockProvider, tokensStore, baseHttpClientFactory, expressionEvaluator);
  }

  @Override
  public TestOAuthDancer build() {
    // There are some minimal configuration parameters that are required for the dancer to be able to start.
    OAuthDancerConfig dancerConfig = new DefaultClientCredentialsOAuthDancerConfig();
    dancerConfig.setName(name);
    dancerConfig.setResourceOwnerIdTransformer(resourceOwnerIdTransformer);
    dancerConfig.setLockProvider(lockProvider);
    dancerConfig.setTokensStore(tokensStore);
    dancerConfig.setSchedulerService(schedulerService);
    dancerConfig.setHttpClient(httpClientFactory.get());
    return new TestOAuthDancer(dancerConfig);
  }
}
