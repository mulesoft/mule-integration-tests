/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.context.annotation.Description;

import io.qameta.allure.Issue;

@Ignore("MULE-14827")
@Issue("MULE-14827")
public class SmartConnectorUsesAppConfigTestCase extends AbstractIntegrationTestCase {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/lifecycle/smart-connector-uses-app-config.xml";
  }

  @Test
  public void request() throws Exception {
    // Do the OAuth dance
    flowRunner("store-auth-code").run();

    // Use the SC to do a request authenticated with OAuth
    flowRunner("request").run();
  }
}
