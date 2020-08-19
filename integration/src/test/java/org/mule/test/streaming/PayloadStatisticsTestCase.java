/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.tck.util.StatisticsEnabled;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.tests.api.TestQueueManager;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Named;

import org.junit.After;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsTestCase extends AbstractIntegrationTestCase {

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Inject
  private TestQueueManager queueManager;

  @Inject
  @Named("listOfMessagesSource")
  public Flow listOfMessagesSource;

  public StatisticsEnabled withStatistics;

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/payload-statistics-config.xml";
  }

  @Before
  public void before() throws Throwable {
    withStatistics = new StatisticsEnabled(() -> muleContext);
    withStatistics.before();
  }

  @After
  public void after() {
    withStatistics.after();
  }

  @Test
  @Description("Assert statistics for a source that generates a List of objects with an iterator")
  public void listOfMessagesSource() throws MuleException {
    listOfMessagesSource.start();

    queueManager.read("listOfMessagesSourceComplete", RECEIVE_TIMEOUT, MILLISECONDS).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("listOfMessagesSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:cerebro-detect-new-mutants"));
    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    // do not count the container message
    assertThat(fileListStatistics.getOutputObjectCount(), is(3L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));

  }

  @Test
  @Description("Assert statistics for an operation that generates a List of objects with a stream")
  public void listOfMessagesOperation() throws Exception {
    flowRunner("listOfMessagesOperation").run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("listOfMessagesOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:adamantium-injectors"));
    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    // do not count the container message
    assertThat(fileListStatistics.getOutputObjectCount(), is(4L));
    assertThat(fileListStatistics.getOutputByteCount(), is(1024 * 1024 * 4L));

  }

  @Test
  @Description("Assert statistics for an operation that returns a PagingProvider of objects with a stream")
  public void pagesOfMessagesOperation() throws Exception {
    for (int i = 0; i < 100; ++i) {
      writeStringToFile(new File(temporaryFolder.getRoot(), "file_" + i + ".txt"), randomAlphanumeric(1024 * 1024));
    }

    flowRunner("pagesOfMessagesOperation").withVariable("path", temporaryFolder.getRoot().getPath()).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("pagesOfMessagesOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("file:list"));
    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(100L));
    // TODO MULE-18650 the operation used in this test returns a PagingProvider, which needs specific handling
    // assertThat(fileListStatistics.getOutputByteCount(), is(1024 * 1024 * 100L));
  }
}
