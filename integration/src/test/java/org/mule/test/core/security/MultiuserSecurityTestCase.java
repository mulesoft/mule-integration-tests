/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.security;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mule.functional.security.TestSingleUserSecurityProvider.PROPERTY_FAVORITE_COLOR;
import static org.mule.functional.security.TestSingleUserSecurityProvider.PROPERTY_NUMBER_LOGINS;
import static org.mule.runtime.core.api.config.MuleProperties.MULE_USER_PROPERTY;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.security.Authentication;
import org.mule.runtime.core.api.InternalEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.security.DefaultMuleCredentials;
import org.mule.runtime.core.api.security.EncryptionStrategy;
import org.mule.runtime.core.api.security.SecurityContext;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Tests multi-user security against a security provider which only authenticates a single user at a time (i.e., authentication of
 * a new user overwrites the previous authentication).
 *
 * see EE-979
 */
@Ignore
public class MultiuserSecurityTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String[] getConfigFiles() {
    return new String[] {"multiuser-security-test-flow.xml", "singleuser-security-provider.xml"};
  }

  @Test
  public void testMultipleAuthentications() throws Exception {
    Message reply;

    reply = getResponse("Data1", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 1, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data2", "stan");
    assertNotNull(reply);
    assertEquals("user = stan, logins = 1, color = metallic blue", reply.getPayload().getValue());

    reply = getResponse("Data3", "cindy");
    assertEquals("user = cindy, logins = 1, color = dark violet", reply.getPayload().getValue());

    reply = getResponse("Data4", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 2, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data4", "marie");
    assertNotNull(reply);
    assertEquals("user = marie, logins = 3, color = bright red", reply.getPayload().getValue());

    reply = getResponse("Data2", "stan");
    assertNotNull(reply);
    assertEquals("user = stan, logins = 2, color = metallic blue", reply.getPayload().getValue());
  }

  public Message getResponse(String data, String user) throws Exception {
    EncryptionStrategy strategy = muleContext.getSecurityManager().getEncryptionStrategy("PBE");

    Map<String, Serializable> props = new HashMap<>();
    props.put(MULE_USER_PROPERTY, DefaultMuleCredentials.createHeader(user, user, "PBE", strategy));
    return flowRunner("testService").withPayload(data).withInboundProperties(props).run().getMessage();
  }

  public static class TestSecurityProcessor implements Processor {

    protected static final Logger logger = LoggerFactory.getLogger(TestSecurityProcessor.class);

    @Override
    public InternalEvent process(InternalEvent event) throws MuleException {
      SecurityContext securityContext = event.getSecurityContext();
      Authentication authentication = securityContext.getAuthentication();

      int numberLogins = (Integer) authentication.getProperties().get(PROPERTY_NUMBER_LOGINS);
      String favoriteColor = (String) authentication.getProperties().get(PROPERTY_FAVORITE_COLOR);

      String msg = "user = " + authentication.getPrincipal() + ", logins = " + numberLogins + ", color = " + favoriteColor;
      logger.debug(msg);

      return InternalEvent.builder(event).message(Message.builder(event.getMessage()).value(msg).build()).build();
    }
  }
}
