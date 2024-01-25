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
import static java.lang.Thread.sleep;

import static org.apache.commons.io.FileUtils.copyFileToDirectory;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

import io.qameta.allure.Description;
import org.junit.rules.ExpectedException;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;

@Feature(INTEGRATIONS_TESTS)
@Story(SINGLE_APP_DEPLOYMENT)
public class SingleAppModeTestCase extends AbstractFakeMuleServerTestCase {

  public static final String SIMPLE_APP_NAME = "simple-app";

  public static final String SECOND_SIMPLE_APP_NAME = "second-simple-app";

  public static final String SIMPLE_APP_NAME_APP_DEPLOYMENT = "simple-app-1.0.0-mule-application";
  public static final String SIMPLE_APP_ROUTE = "apps/simple-app/simple-app.xml";
  public static final String SIMPLE_APP_LOG_CONF_ROUTE = "apps/simple-app/log4j2.xml";

  public static final long TIMEOUT = 5000;
  public static final long POLLING_INTERVAL = 1000;

  @Rule
  public ExpectedException expected = ExpectedException.none();
  @Rule
  public SystemProperty singleAppMode = new SystemProperty(SINGLE_APP_MODE_PROPERTY, TRUE.toString());

  @Test
  @Description("An app can be deployed through the deployment service")
  public void oneAppCanBeDeployedThroughDeploymentService() throws Exception {
    muleServer.start();
    muleServer.deploy(new ApplicationFileBuilder(SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile().toURI().toURL(), SIMPLE_APP_NAME);

    assertThat(muleServer.findApplication(SIMPLE_APP_NAME), is(notNullValue()));
  }

  @Test
  @Description("When a file is placed in the apps folder it will be deployed")
  public void oneAppCanBeDeployedByPlacingItInTheAppsFolder() throws Exception {
    copyFileToDirectory(new ApplicationFileBuilder(SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile(),
                        muleServer.getAppsDir());
    muleServer.start();
    probe(TIMEOUT, POLLING_INTERVAL, () -> muleServer.findApplication(SIMPLE_APP_NAME_APP_DEPLOYMENT) != null);
  }

  @Test
  @Description("When one app is deployed, placing a file in the /apps folder will have no effect.")
  public void oneAppCanBeDeployedByPlacingItInTheAppsFolderAndTheSecondAppIsNotDeployed() throws Exception {
    copyFileToDirectory(new ApplicationFileBuilder(SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile(),
                        muleServer.getAppsDir());
    muleServer.start();
    probe(TIMEOUT, POLLING_INTERVAL, () -> muleServer.findApplication(SIMPLE_APP_NAME_APP_DEPLOYMENT) != null);

    copyFileToDirectory(new ApplicationFileBuilder(SECOND_SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile(),
                        muleServer.getAppsDir());

    // Wait to verify that the app was not deployed.
    sleep(TIMEOUT);

    assertThat(muleServer.findApplication(SECOND_SIMPLE_APP_NAME), is(nullValue()));
  }

  @Test
  @Description("When one app is deployed, an attempt to deploy through the deployment service must fail")
  public void oneAppCanBeDeployedByPlacingItInTheAppsFolderAndTheSecondAppFailsToBeDeployedInDeploymentService()
      throws Exception {
    expected.expectMessage("Failed to deploy application: second-simple-app");
    muleServer.start();
    muleServer.deploy(new ApplicationFileBuilder(SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile().toURI().toURL(), SIMPLE_APP_NAME);
    assertThat(muleServer.findApplication(SIMPLE_APP_NAME), is(notNullValue()));

    muleServer.deploy(new ApplicationFileBuilder(SECOND_SIMPLE_APP_NAME).definedBy(SIMPLE_APP_ROUTE)
        .containingResource(SIMPLE_APP_LOG_CONF_ROUTE, "log4j2.xml").getArtifactFile().toURI().toURL(), SECOND_SIMPLE_APP_NAME);
  }

}
