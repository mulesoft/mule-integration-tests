/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import org.mule.functional.api.component.TestConnectorQueueHandler;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.File;
import java.io.IOException;

import javax.inject.Inject;
import javax.inject.Named;

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

  public static final int BYTES_SIZE = 1343;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Rule
  public SystemProperty bytesSize = new SystemProperty("bytesSize", "" + BYTES_SIZE);

  @Rule
  public SystemProperty withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  private TestConnectorQueueHandler queueHandler;

  @Inject
  @Named("listOfMessagesSource")
  public Flow listOfMessagesSource;

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/payload-statistics-config.xml";
  }

  @Before
  public void before() throws IOException {
    cleanDirectory(temporaryFolder.getRoot());
    queueHandler = new TestConnectorQueueHandler(registry);
  }

  @Test
  @Description("Assert statistics for a source that generates a List of objects with an iterator")
  public void listOfMessagesSource() throws MuleException {
    listOfMessagesSource.start();

    queueHandler.read("listOfMessagesSourceComplete", RECEIVE_TIMEOUT).getMessage();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("listOfMessagesSource/source");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:cerebro-detect-new-mutants"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

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

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    // do not count the container message
    assertThat(fileListStatistics.getOutputObjectCount(), is(4L));
    assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 4L));

  }

  @Test
  @Description("Assert statistics for an operation that returns a PagingProvider of objects with a stream")
  public void pagesOfMessagesOperation() throws Exception {
    for (int i = 0; i < 3; ++i) {
      writeStringToFile(new File(temporaryFolder.getRoot(), "file_" + i + ".txt"), randomAlphanumeric(BYTES_SIZE));
    }

    flowRunner("pagesOfMessagesOperation").withVariable("path", temporaryFolder.getRoot().getPath()).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("pagesOfMessagesOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("file:list"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(3L));
    // TODO MULE-18652 the operation used in this test returns a PagingProvider, which needs specific handling
    // assertThat(fileListStatistics.getOutputByteCount(), is(BYTES_SIZE * 3L));
  }
}
