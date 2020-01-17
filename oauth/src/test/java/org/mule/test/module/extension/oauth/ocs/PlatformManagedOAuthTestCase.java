/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthPlatformManagedDancerBuilder;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.List;
import java.util.Map;

/**
 * Base class for Test cases that use the Platform Managed OAuth connection features.
 * <p>
 * It replaces the {@link OAuthService} in the {@link MuleRegistry} with {@link #mockOAuthService} which returns
 * {@link #mockPlatformManagedDancerBuilder} when
 * {@link OAuthService#platformManagedOAuthDancerBuilder(LockFactory, Map, MuleExpressionLanguage)} is invoked. Said mock
 * builder also returns {@link #mockPlatformDancer} upon {@link OAuthPlatformManagedDancerBuilder#build()}.
 * <p>
 * Additional stubs can be added on each specific test.
 *
 * @since 4.3.0
 */
public abstract class PlatformManagedOAuthTestCase extends BaseOAuthExtensionTestCase {

  protected OAuthPlatformManagedDancerBuilder mockPlatformManagedDancerBuilder =
      mock(OAuthPlatformManagedDancerBuilder.class, RETURNS_DEEP_STUBS);
  protected PlatformManagedOAuthDancer mockPlatformDancer = mock(PlatformManagedOAuthDancer.class, RETURNS_DEEP_STUBS);
  protected OAuthService mockOAuthService = mock(OAuthService.class);

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    when(mockOAuthService.platformManagedOAuthDancerBuilder(any(), any(), any())).thenReturn(mockPlatformManagedDancerBuilder);
    when(mockPlatformManagedDancerBuilder.build()).thenReturn(mockPlatformDancer);

    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().overrideDefaultServiceImpl("OAUTH service - OAuthService", mockOAuthService);
      }
    });
  }
}
