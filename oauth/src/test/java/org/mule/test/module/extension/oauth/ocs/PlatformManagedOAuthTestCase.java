/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.ocs;

import static java.util.Arrays.asList;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import static org.mule.oauth.client.api.state.DancerState.HAS_TOKEN;
import static org.mule.runtime.extension.internal.ocs.OCSConstants.OCS_ENABLED;
import static org.mule.test.allure.AllureConstants.OauthFeature.OCS_SUPPORT;

import org.mule.oauth.client.api.state.DancerState;
import org.mule.oauth.client.api.state.ResourceOwnerOAuthContext;
import org.mule.runtime.api.el.MuleExpressionLanguage;
import org.mule.runtime.api.lock.LockFactory;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.runtime.core.api.config.builders.AbstractConfigurationBuilder;
import org.mule.runtime.core.internal.registry.MuleRegistry;
import org.mule.runtime.oauth.api.OAuthService;
import org.mule.runtime.oauth.api.PlatformManagedConnectionDescriptor;
import org.mule.runtime.oauth.api.PlatformManagedOAuthDancer;
import org.mule.runtime.oauth.api.builder.OAuthPlatformManagedDancerBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.runners.Parameterized;

import io.qameta.allure.Feature;

/**
 * Base class for Test cases that use the Platform Managed OAuth connection features.
 * <p>
 * It replaces the {@link OAuthService} in the {@link MuleRegistry} with {@link #mockOAuthService} which returns
 * {@link #mockPlatformManagedDancerBuilder} when
 * {@link OAuthService#platformManagedOAuthDancerBuilder(LockFactory, Map, MuleExpressionLanguage)} is invoked. Said mock builder
 * also returns {@link #mockPlatformDancer} upon {@link OAuthPlatformManagedDancerBuilder#build()}.
 * <p>
 * Additional stubs can be added on each specific test.
 *
 * @since 4.3.0
 */

@Feature(OCS_SUPPORT)
@RunnerDelegateTo(Parameterized.class)
public abstract class PlatformManagedOAuthTestCase extends BaseOAuthExtensionTestCase {

  protected static final String TEST_ACCESS_TOKEN = "accessTokenTest";
  protected static final String EXPIRES_IN_TEST = "expiresInTest";

  protected static final String USER_ID_PARAMETER_NAME = "userId";
  protected static final String USER_ID_TEST = "userIdTest";
  protected static final String INSTANCE_ID_TEST = "instanceIdTest";
  protected static final String INSTANCE_ID_PARAMETER_NAME = "instanceId";
  protected static final String DISPLAY_PARAMETER_NAME = "display";
  protected static final String DISPLAY_TEST = "displayTestValue";
  protected static final String API_VERSION_PARAMETER_NAME = "apiVersion";
  protected static final String PROMPT_PARAMETER_NAME = "prompt";
  protected static final boolean PROMPT_TEST = false;
  protected static final Double API_VERSION_TEST = new Double(5.3);

  @ClassRule
  public static SystemProperty enableOcs = new SystemProperty(OCS_ENABLED, "true");

  protected OAuthPlatformManagedDancerBuilder mockPlatformManagedDancerBuilder =
      mock(OAuthPlatformManagedDancerBuilder.class, RETURNS_DEEP_STUBS);
  protected PlatformManagedOAuthDancer mockPlatformDancer = mock(PlatformManagedOAuthDancer.class, RETURNS_DEEP_STUBS);
  protected OAuthService mockOAuthService = mock(OAuthService.class);
  protected PlatformManagedConnectionDescriptor mockPlatformManagedConnectionDescriptor =
      mock(PlatformManagedConnectionDescriptor.class);


  @Parameterized.Parameter(0)
  public String name;

  @Parameterized.Parameter(1)
  public String configFile;

  @Parameterized.Parameter(2)
  public boolean usesAuthorizationCodeProvider;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"Client Credentials configuration", "ocs/client-credentials-platform-managed-config.xml", false},
        {"Authorization Code configuration",
            "ocs/authorization-code-platform-managed-config.xml", true},
        {"Resolved configuration", "ocs/resolved-platform-managed-config.xml", true}
    });
  }

  @Override
  protected String[] getConfigFiles() {
    return new String[] {configFile, "ocs/platform-managed-flows.xml"};
  }

  @Override
  protected void addBuilders(List<ConfigurationBuilder> builders) {
    super.addBuilders(builders);
    when(mockOAuthService.platformManagedOAuthDancerBuilder(any(), any(), any())).thenReturn(mockPlatformManagedDancerBuilder);
    when(mockPlatformManagedDancerBuilder.build()).thenReturn(mockPlatformDancer);

    builders.add(new AbstractConfigurationBuilder() {

      @Override
      protected void doConfigure(MuleContext muleContext) throws Exception {
        muleContext.getCustomizationService().overrideDefaultServiceImpl("OAUTH service - OAuthService", mockOAuthService);
      }
    });
  }

  @Override
  protected boolean isDisposeContextPerClass() {
    return false;
  }

  @Override
  @Before
  public void doSetUpBeforeMuleContextCreation() throws Exception {
    CompletableFuture<PlatformManagedConnectionDescriptor> connectionDescriptorCompletableFuture = mock(CompletableFuture.class);
    when(mockPlatformDancer.getConnectionDescriptor()).thenReturn(connectionDescriptorCompletableFuture);
    when(connectionDescriptorCompletableFuture.get()).thenReturn(mockPlatformManagedConnectionDescriptor);

    when(mockPlatformManagedConnectionDescriptor.getParameters()).thenReturn(getDescriptorParameters());

    CompletableFuture<String> accessTokenCompletableFuture = mock(CompletableFuture.class);
    when(mockPlatformDancer.accessToken()).thenReturn(accessTokenCompletableFuture);
    when(accessTokenCompletableFuture.get()).thenReturn(getAccessToken());
    ResourceOwnerOAuthContext resourceOwnerOAuthContext = mock(ResourceOwnerOAuthContext.class);

    when(mockPlatformDancer.getContext()).thenReturn(resourceOwnerOAuthContext);

    when(resourceOwnerOAuthContext.getAccessToken()).thenReturn(getAccessToken());
    when(resourceOwnerOAuthContext.getDancerState()).thenReturn(getDancerState());
    when(resourceOwnerOAuthContext.getExpiresIn()).thenReturn(getExpiresIn());


    when(resourceOwnerOAuthContext.getTokenResponseParameters()).thenReturn(getTokenResponseParameters());
  }

  protected Map<String, Object> getDescriptorParameters() {
    Map<String, Object> parameters = new HashMap<>();
    parameters.put(DISPLAY_PARAMETER_NAME, DISPLAY_TEST);
    parameters.put(API_VERSION_PARAMETER_NAME, API_VERSION_TEST);
    parameters.put(PROMPT_PARAMETER_NAME, PROMPT_TEST);

    return parameters;
  }

  protected String getAccessToken() {
    return TEST_ACCESS_TOKEN;
  }

  protected DancerState getDancerState() {
    return HAS_TOKEN;
  }

  protected String getExpiresIn() {
    return EXPIRES_IN_TEST;
  }

  protected Map<String, Object> getTokenResponseParameters() {
    Map<String, Object> tokenResponseParameters = new HashMap<>();
    tokenResponseParameters.put(USER_ID_PARAMETER_NAME, USER_ID_TEST);
    tokenResponseParameters.put(INSTANCE_ID_PARAMETER_NAME, INSTANCE_ID_TEST);
    return tokenResponseParameters;
  }

  @Override
  protected boolean mustRegenerateExtensionModels() {
    return true;
  }

}
