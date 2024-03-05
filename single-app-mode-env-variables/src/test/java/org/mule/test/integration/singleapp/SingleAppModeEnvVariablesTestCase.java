/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.singleapp;

import static org.mule.runtime.api.util.MuleSystemProperties.SINGLE_APP_MODE_PROPERTY;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.ArtifactDeploymentFeature.SingleAppDeploymentStory.SINGLE_APP_DEPLOYMENT;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;

import static java.lang.Boolean.TRUE;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;

import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(INTEGRATIONS_TESTS)
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppModeEnvVariablesTestCase extends AbstractFakeMuleServerTestCase {

  private static final long TIMEOUT = 5000;
  private static final long POLLING_INTERVAL = 1000;

  private static final String SINGLE_APP_NAME = "single-app";
  private static final String SIMPLE_APP_NAME = "simple-app";
  private static final String SIMPLE_APP_ROUTE = "apps/simple-app/simple-app.xml";
  private static final String SIMPLE_APP_LOG_CONF_ROUTE = "apps/simple-app/log4j2.xml";
  private static final String LOG_CONF_FILENAME = "log4j2.xml";

  @Rule
  public SystemProperty singleAppMode = new SystemProperty(SINGLE_APP_MODE_PROPERTY, TRUE.toString());

  @Test
  @Description("When the MULE_APPS_PATH environment variable is set, it deploys that application.")
  public void whenTheMuleAppsPathEnvironmentVariableIsSetItDeploysThatApp() throws Exception {
    muleServer.start();
    muleServer.assertDeploymentSuccess(SINGLE_APP_NAME);
    probe(TIMEOUT, POLLING_INTERVAL, () -> muleServer.findApplication(SINGLE_APP_NAME) != null);
  }

  @Test
  @Description("When the MULE_APPS_PATH environment variable is set, it deploys that application instead of the one placed in the apps folder.")
  public void whenTheMuleAppsPathEnvironmentVariableIsSetItDeploysThatAppInsteadTheOnePlacedInTheAppsFolder() throws Exception {
    copyFileToDirectory(new ApplicationFileBuilder(SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, LOG_CONF_FILENAME).getArtifactFile(),
                        muleServer.getAppsDir());
    muleServer.start();
    muleServer.assertDeploymentSuccess(SINGLE_APP_NAME);
    probe(TIMEOUT, POLLING_INTERVAL, () -> (muleServer.findApplication(SINGLE_APP_NAME) != null) &&
        (muleServer.findApplication(SIMPLE_APP_NAME) == null));
  }
}
