/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.security;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.exception.ExceptionHelper.getRootException;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_EXPRESSION_MANAGER;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.api.security.SecurityContext;
import org.mule.runtime.api.security.SecurityProviderNotFoundException;
import org.mule.runtime.api.security.UnknownAuthenticationTypeException;
import org.mule.runtime.core.api.el.ExpressionManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.security.AbstractAuthenticationFilter;
import org.mule.runtime.core.api.security.CryptoFailureException;
import org.mule.runtime.core.api.security.EncryptionStrategyNotFoundException;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

import org.junit.Test;
import org.springframework.security.authentication.BadCredentialsException;

/**
 * See MULE-4916: spring beans inside a security filter
 */
public class CustomSecurityFilterTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  private static final String EXPECTED_PASSWORD = "ross";

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/security/custom-security-filter-test.xml";
  }

  @Test
  public void testOutboundAutenticationSend() throws Exception {
    Map<String, Serializable> props = new HashMap<>();
    props.put("username", "ross");
    props.put("pass", EXPECTED_PASSWORD);

    CoreEvent event = flowRunner("test").withPayload("hi").withInboundProperties(props).run();

    assertThat(event.getError().isPresent(), is(false));

    props.put("pass", "badpass");

    Exception e = flowRunner("test").withPayload("hi").withInboundProperties(props).runExpectingException();
    assertThat(getRootException(e), instanceOf(BadCredentialsException.class));
  }

  public static class CustomSecurityFilter extends AbstractAuthenticationFilter {

    private String password;

    @Override
    public SecurityContext authenticate(CoreEvent event) throws SecurityException, UnknownAuthenticationTypeException,
        CryptoFailureException, SecurityProviderNotFoundException, EncryptionStrategyNotFoundException, InitialisationException {
      ExpressionManager expressionManager = (ExpressionManager) registry.lookupByName(OBJECT_EXPRESSION_MANAGER).get();

      Object passwordEval = expressionManager.evaluate(password, event).getValue();
      if (!passwordEval.equals(EXPECTED_PASSWORD)) {
        throw new BadCredentialsException("Bad credentials");
      }

      return event.getSecurityContext();
    }

    public void setPassword(String password) {
      this.password = password;
    }
  }
}
