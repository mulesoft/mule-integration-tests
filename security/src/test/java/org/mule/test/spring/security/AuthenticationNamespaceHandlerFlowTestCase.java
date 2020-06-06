/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring.security;

import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;

import org.mule.extension.spring.api.security.PreAuthenticatedAuthenticationProvider;
import org.mule.extension.spring.api.security.SpringProviderAdapter;
import org.mule.extension.spring.api.security.UserAndPasswordAuthenticationProvider;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.Component;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;

public class AuthenticationNamespaceHandlerFlowTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort port1 = new DynamicPort("port1");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/spring/security/authentication-config-flow.xml";
  }

  @Test
  public void testSecurityManagerConfigured() {
    SecurityManager securityManager = registry.<SecurityManager>lookupByName(OBJECT_SECURITY_MANAGER).get();
    assertNotNull(securityManager);

    Collection<SecurityProvider> providers = securityManager.getProviders();
    assertThat(providers, hasSize(2));

    assertThat(containsSecurityProvider(providers, UserAndPasswordAuthenticationProvider.class), is(true));
    assertThat(containsSecurityProvider(providers, PreAuthenticatedAuthenticationProvider.class), is(true));
  }

  private boolean containsSecurityProvider(Collection<SecurityProvider> providers, Class authenticationProviderClass) {
    for (SecurityProvider provider : providers) {
      assertThat(provider, instanceOf(SpringProviderAdapter.class));
      assertThat(provider, instanceOf(Component.class));
      if (authenticationProviderClass
          .isAssignableFrom(((SpringProviderAdapter) provider).getAuthenticationProvider().getClass())) {
        return true;
      }
    }
    return false;
  }
}
