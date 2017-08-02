/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.slf4j.LoggerFactory.getLogger;
import org.mule.runtime.config.spring.CustomSecurityProviderDelegate;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.runtime.core.api.security.SecurityProvider;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Iterator;

import org.junit.Test;
import org.slf4j.Logger;

public class SecurityNamespaceHandlerTestCase extends AbstractIntegrationTestCase {

  private static final Logger LOGGER = getLogger(SecurityNamespaceHandlerTestCase.class);

  @Override
  protected String getConfigFile() {
    return "security-namespace-config.xml";
  }

  @Test
  public void testSecurity() {
    SecurityManager securityManager = muleContext.getSecurityManager();
    SecurityProvider dummySecurityProvider = securityManager.getProvider("dummySecurityProvider");
    assertNotNull(dummySecurityProvider);
    assertTrue(dummySecurityProvider instanceof CustomSecurityProviderDelegate);
    verifyEncryptionStrategy(securityManager, "dummyEncryptionStrategy",
                             "org.mule.runtime.config.spring.CustomEncryptionStrategyDelegate");
    verifyEncryptionStrategy(securityManager, "passwordEncryptionStrategy",
                             "org.mule.runtime.core.internal.security.PasswordBasedEncryptionStrategy");
    verifyEncryptionStrategy(securityManager, "secretKeyEncryptionStrategy",
                             "org.mule.runtime.core.internal.security.SecretKeyEncryptionStrategy");
  }

  private void verifyEncryptionStrategy(SecurityManager securityManager, String name, String className) {
    Iterator strategies = securityManager.getEncryptionStrategies().iterator();
    LOGGER.debug("Listing strategies");
    while (strategies.hasNext()) {
      EncryptionStrategy strategy = (EncryptionStrategy) strategies.next();
      LOGGER.debug(strategy.getName() + " / " + strategy.toString() + " / " + strategy.getClass());
    }
    assertNotNull(name, securityManager.getEncryptionStrategy(name));
    assertThat(securityManager.getEncryptionStrategy(name).getClass().getName(), equalTo(className));
  }
}
