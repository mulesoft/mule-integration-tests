/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import org.mule.runtime.core.api.config.i18n.CoreMessages;
import org.mule.runtime.api.i18n.I18nMessage;
import org.mule.tck.junit4.AbstractMuleTestCase;

import java.util.MissingResourceException;

import org.junit.Test;

public class I18nMessagesTestCase extends AbstractMuleTestCase {

  @Test
  public void testMessageLoading() throws Exception {
    I18nMessage message = CoreMessages.authFailedForUser("Fred");
    assertEquals("Authentication failed for principal Fred", message.getMessage());
    assertEquals(135, message.getCode());
  }

  @Test
  public void testBadBundle() {
    try {
      InvalidMessageFactory.getInvalidMessage();
      fail("should throw resource bundle not found exception");
    } catch (MissingResourceException e) {
      // IBM JDK6: Can't find resource for bundle ...
      // Sun/IBM JDK5: Can't find bundle for base name ...
      assertTrue(e.getMessage().matches(".*Can't find.*bundle.*"));
    }
  }

  @Test
  public void testGoodBundle() {
    I18nMessage message = TestI18nMessages.testMessage("one", "two", "three");
    assertEquals("Testing, Testing, one, two, three", message.getMessage());
    assertEquals(1, message.getCode());
  }
}
