/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.lifecycle;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.connection.ConnectionValidationResult;
import org.mule.runtime.api.connectivity.ConnectivityTestingService;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Issue;

@Issue("MULE-14827")
public class SmartConnectorUsesAppConfigTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort listenPort = new DynamicPort("port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");

  @Inject
  private ConnectivityTestingService connectivityTestingService;

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

  @Test
  public void testConnection() {
    ConnectionValidationResult result =
        connectivityTestingService.testConnection(Location.builder().globalName("scConfig").build());
    assertThat(result.isValid(), is(true));
  }
}
