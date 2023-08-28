/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.context.notification;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.containsString;
import static org.junit.rules.ExpectedException.none;

import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;
import java.util.function.Consumer;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class NotificationTypesTestCase extends AbstractIntegrationTestCase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"org/mule/test/context/notification/mule-config-inexistent-notification.xml",
            (Consumer<ExpectedException>) expected -> expected
                .expectMessage(containsString("No notification 'nothing:I-DONT-EXIST' declared in this applications plugins to enable."))},

        {"org/mule/test/context/notification/mule-config-disable-inexistent-notification.xml",
            (Consumer<ExpectedException>) expected -> expected
                .expectMessage(containsString("No notification 'nothing:I-DONT-EXIST' declared in this applications plugins to disable."))},

        {"org/mule/test/context/notification/mule-config-invalid-notification.xml",
            (Consumer<ExpectedException>) expected -> expected
                .expectMessage(containsString("'1234' is not a valid value of union type 'notificationTypes'"))},

        {"org/mule/test/context/notification/mule-config-disable-invalid-notification.xml",
            (Consumer<ExpectedException>) expected -> expected
                .expectMessage(containsString("'1234' is not a valid value of union type 'notificationTypes'"))}
    });
  }

  @Rule
  public ExpectedException expected;

  private String configFile;

  public NotificationTypesTestCase(String configFile, Consumer<ExpectedException> expectedConfigurer) {
    this.configFile = configFile;
    expected = none();
    expectedConfigurer.accept(expected);
  }

  @Override
  protected String getConfigFile() {
    return configFile;
  }

  @Test
  public void failingNotificationType() {
    // Nothing to do. Te test will fail on setup.
  }
}
