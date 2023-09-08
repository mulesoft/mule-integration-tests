/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.connection;

import static java.util.Optional.of;
import static org.mule.runtime.core.api.config.MuleDeploymentProperties.MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY;
import static org.mule.runtime.core.api.error.Errors.Identifiers.CONNECTIVITY_ERROR_IDENTIFIER;
import static org.mule.tck.junit4.matcher.ErrorTypeMatcher.errorType;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.DEPLOYMENT_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.DeploymentConfiguration.LazyConnectionsStory.LAZY_CONNECTIONS;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.interception.InterceptionAction;
import org.mule.runtime.api.interception.InterceptionEvent;
import org.mule.runtime.api.interception.ProcessorInterceptor;
import org.mule.runtime.api.interception.ProcessorInterceptorFactory;
import org.mule.runtime.api.interception.ProcessorParameterValue;
import org.mule.test.AbstractIntegrationTestCase;

import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(DEPLOYMENT_CONFIGURATION)
@Story(LAZY_CONNECTIONS)
public class LazyConnectionsTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/connection/lazy-connections-config.xml";
  }

  @Test
  public void executedOperationThrowsConnectivityError() throws Exception {
    testPetStoreFlow("skipOperationFlowInvalidConfig");
    testPetStoreFlow("skipOperationFlowInvalidConfigFailsDeployment");
  }

  @Test
  public void executeDbOperation() throws Exception {
    testDbFlow("dbOperationFlow");
  }

  private void testPetStoreFlow(String flowName) throws Exception {
    flowRunner(flowName).withVariable("execute", true)
        .runExpectingException(errorType("PETSTORE", CONNECTIVITY_ERROR_IDENTIFIER));
  }

  private void testDbFlow(String flowName) throws Exception {
    flowRunner(flowName).withVariable("execute", true).run();
  }

  @Override
  protected Optional<Properties> getDeploymentProperties() {
    Properties properties = new Properties();
    properties.put(MULE_LAZY_CONNECTIONS_DEPLOYMENT_PROPERTY, "true");
    return of(properties);
  }

  public static class TestInterceptorFactory implements ProcessorInterceptorFactory {

    @Override
    public ProcessorInterceptor get() {
      return new ProcessorInterceptor() {

        @Override
        public CompletableFuture<InterceptionEvent> around(ComponentLocation location,
                                                           Map<String, ProcessorParameterValue> parameters,
                                                           InterceptionEvent event, InterceptionAction action) {
          if (location.getComponentIdentifier().getIdentifier().getNamespace().equalsIgnoreCase("db")) {
            return action.skip();
          }
          return action.proceed();
        }
      };
    }
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
