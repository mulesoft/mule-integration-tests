/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config;

import static org.apache.commons.lang3.reflect.FieldUtils.readField;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.Iterator;

import org.slf4j.Logger;

import org.junit.Test;

public class SecurityNamespaceHandlerTestCase extends MuleArtifactFunctionalTestCase implements IntegrationTestCaseRunnerConfig {

  private static final Logger LOGGER = getLogger(SecurityNamespaceHandlerTestCase.class);

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/security-namespace-config.xml";
  }

  @Test
  public void testSecurity() throws Exception {
    SecurityManager securityManager = muleContext.getSecurityManager();
    SecurityProvider dummySecurityProvider = securityManager.getProvider("dummySecurityProvider");
    assertNotNull(dummySecurityProvider);
    assertThat(dummySecurityProvider.getClass().getName(),
               is("org.mule.runtime.config.internal.bean.CustomSecurityProviderDelegate"));
    verifyEncryptionStrategy(securityManager, "dummyEncryptionStrategy",
                             "org.mule.runtime.config.internal.bean.CustomEncryptionStrategyDelegate");
    verifyEncryptionStrategy(securityManager, "passwordEncryptionStrategy",
                             "org.mule.runtime.core.internal.security.PasswordBasedEncryptionStrategy");
    verifyEncryptionStrategy(securityManager, "secretKeyEncryptionStrategy",
                             "org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy");
  }

  @Test
  public void testProvidersAreInitialized() throws Exception {
    SecurityManager securityManager = muleContext.getSecurityManager();
    SecurityProvider customDelegateSecurityProvider = securityManager.getProvider("initializableProvider");
    InitTrackerSecurityProvider initTrackerSecurityProvider =
        (InitTrackerSecurityProvider) readField(customDelegateSecurityProvider, "delegate", true);;
    assertThat(initTrackerSecurityProvider.isInitialised(), is(true));
  }

  private void verifyEncryptionStrategy(SecurityManager securityManager, String name, String className) {
    doVerifyEncriptionStrategy(securityManager, name);
    assertThat(securityManager.getEncryptionStrategy(name).getClass().getName(), is(className));
  }

  public void doVerifyEncriptionStrategy(SecurityManager securityManager, String name) {
    Iterator strategies = securityManager.getEncryptionStrategies().iterator();
    LOGGER.debug("Listing strategies");
    while (strategies.hasNext()) {
      EncryptionStrategy strategy = (EncryptionStrategy) strategies.next();
      LOGGER.debug(strategy.getName() + " / " + strategy.toString() + " / " + strategy.getClass());
    }
    assertNotNull(name, securityManager.getEncryptionStrategy(name));
  }

  public static class InitTrackerSecurityProvider implements SecurityProvider, Initialisable {

    private boolean initialised = false;
    private String name;

    @Override
    public String getName() {
      return name;
    }

    @Override
    public void setName(String name) {
      this.name = name;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws SecurityException {
      return null;
    }

    @Override
    public boolean supports(Class<?> aClass) {
      return false;
    }

    @Override
    public SecurityContext createSecurityContext(Authentication auth) throws UnknownAuthenticationTypeException {
      return null;
    }

    @Override
    public void initialise() throws InitialisationException {
      this.initialised = true;
    }

    public boolean isInitialised() {
      return this.initialised;
    }
  }
}
