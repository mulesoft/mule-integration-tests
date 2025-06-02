/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.logging;

import static org.mule.runtime.core.api.alert.MuleAlertingSupport.AlertNames.ALERT_ASYNC_LOGGER_RINGBUFFER_FULL;
import static org.mule.runtime.core.api.util.FileUtils.unzip;
import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.IntegrationTestsFeature.INTEGRATIONS_TESTS;
import static org.mule.test.allure.AllureConstants.Logging.LOGGING;
import static org.mule.test.allure.AllureConstants.Logging.LoggingStory.LOGGING_LIBS_SUPPORT;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SUPPORTABILITY;
import static org.mule.test.allure.AllureConstants.SupportabilityFeature.SupportabilityStory.ALERTS;

import static java.lang.System.getProperty;
import static java.nio.file.Files.createTempDirectory;
import static java.util.Objects.requireNonNull;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.core.api.alert.MuleAlertingSupport;
import org.mule.runtime.module.deployment.impl.internal.builder.ApplicationFileBuilder;
import org.mule.runtime.module.log4j.api.MuleAlertingAsyncQueueFullPolicy;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.CompilerUtils;
import org.mule.test.infrastructure.deployment.AbstractFakeMuleServerTestCase;

import java.io.File;
import java.io.IOException;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(INTEGRATIONS_TESTS)
@Feature(LOGGING)
@Feature(SUPPORTABILITY)
@Story(LOGGING_LIBS_SUPPORT)
@Story(ALERTS)
public class Log4jAsyncRingbufferFullAlertTestCase extends AbstractFakeMuleServerTestCase {

  public static final String APP_NAME = "app_ringbuffer-full";

  @BeforeClass
  public static void setUpELService() throws IOException {
    testServicesSetup
        .overrideExpressionLanguageService(Log4jAsyncRingbufferFullAlertTestCase::getRealExpressionLanguageServiceFile);
    testServicesSetup.disableExpressionLanguageMetadataService();
  }

  @Rule
  public UseMuleLog4jContextFactory muleLogging = new UseMuleLog4jContextFactory();

  @Rule
  public SystemProperty muleLoggingAsyncQueueFullPolicy =
      new SystemProperty("log4j2.AsyncQueueFullPolicy", MuleAlertingAsyncQueueFullPolicy.class.getName());

  @Test
  public void alertTriggeredForFullRingBuffer() throws Exception {
    File slowLogInterceptor =
        new CompilerUtils.SingleClassCompiler()
            .compile(new File(requireNonNull(Log4jAsyncRingbufferFullAlertTestCase.class
                .getResource("/log/ringbuffer-full/java/com/mycompany/log4jslow/logger/SlowLogInterceptor.java")).toURI()));

    final ApplicationFileBuilder loggingAppFileBuilder =
        new ApplicationFileBuilder(APP_NAME).definedBy("log/ringbuffer-full/mule/log4j-slow-log-app.xml")
            .containingClass(slowLogInterceptor, "com/mycompany/log4jslow/logger/SlowLogInterceptor.class")
            .usingResource("log/ringbuffer-full/resources/log4j2.xml", "log4j2.xml");

    muleServer.start();
    muleServer.deploy(loggingAppFileBuilder.getArtifactFile().toURI().toURL(), APP_NAME);

    MuleAlertingSupport alerting = muleServer.findApplication(APP_NAME).getArtifactContext().getRegistry()
        .lookupByType(MuleAlertingSupport.class).orElseThrow();

    probe(10000, 500, () -> {
      final var timedDataAggregation = alerting.alertsCountAggregation().get(ALERT_ASYNC_LOGGER_RINGBUFFER_FULL);
      return timedDataAggregation != null && timedDataAggregation.forLast60MinsInterval() > 0;
    });
  }

  @Override
  @After
  public void tearDown() throws MuleException {
    final var latchReleaseFlow = (Startable) muleServer.findApplication(APP_NAME).getArtifactContext().getRegistry()
        .lookupByName("latchReleaseFlow").orElseThrow();
    latchReleaseFlow.start();
  }

  @AfterClass
  public static void resetTestServicesSetup() {
    testServicesSetup.reset();
  }

  private static File getRealExpressionLanguageServiceFile(File tempFolder) {
    try {
      // Unpack the service because java doesn't allow to create a classloader with jars within a zip out of the box.
      File serviceExplodedDir = createTempDirectory("mule-service-weave").toFile();

      unzip(new File(getProperty("dataWeaveService")), serviceExplodedDir);
      return serviceExplodedDir;

    } catch (IOException e) {
      throw new IllegalStateException("Couldn't prepare RealExpressionLanguageService.", e);
    }
  }

}
