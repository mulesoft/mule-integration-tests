/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.connection;

import static java.util.Optional.of;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.exception.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.LazyConnectionsStory.LAZY_CONNECTIONS;
import org.mule.tck.junit4.matcher.ErrorTypeMatcher;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Optional;
import java.util.Properties;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Test;

@Feature(DEPLOYMENT_CONFIGURATION)
@Story(LAZY_CONNECTIONS)
public class LazyConnectionsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/connection/lazy-connections-config.xml";
  }

  @Test
  public void executedOperationThrowsConnectivityError() throws Exception {
    testFlow("skipOperationFlowInvalidConfig");
    testFlow("skipOperationFlowInvalidConfigFailsDeployment");
  }

  private void testFlow(String skipOperationFlowInvalidConfig) throws Exception {
    flowRunner(skipOperationFlowInvalidConfig).withVariable("execute", true)
        .runExpectingException(ErrorTypeMatcher.errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));
  }

  @Override
  protected Optional<Properties> getDeploymentProperties() {
    Properties properties = new Properties();
    properties.put(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, "true");
    return of(properties);
  }
}
