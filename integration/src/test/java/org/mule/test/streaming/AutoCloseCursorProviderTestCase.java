/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.streaming;

import static org.mule.tck.probe.PollingProber.probe;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STREAM_MANAGEMENT;

import static org.apache.commons.io.FileUtils.writeStringToFile;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;

import org.mule.runtime.api.component.AbstractComponent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.core.api.streaming.StreamingManager;
import org.mule.runtime.core.api.streaming.StreamingStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.File;

import javax.inject.Inject;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

@Feature(STREAMING)
@Story(STREAM_MANAGEMENT)
@Ignore("TD-0147155")
public class AutoCloseCursorProviderTestCase extends AbstractIntegrationTestCase {

  private static final int OPEN_PROVIDERS = 100;
  private static final int TIMEOUT_MILLIS = 10000;
  private static final int POLL_DELAY_MILLIS = 100;

  private static StreamingStatistics statistics;

  public static class AssertStatisticsProcessor extends AbstractComponent implements Processor {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
        System.gc();

        assertThat(statistics.getClass().getName(), not(containsString("NullStreamingStatistics")));
        assertThat("No cursor provider reclaimed", statistics.getOpenCursorProvidersCount(), is(lessThan(OPEN_PROVIDERS)));

        return true;
      });
      return event;
    }
  }

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Inject
  private StreamingManager streamingManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/auto-close-cursor-provider-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    super.doSetUp();
    statistics = streamingManager.getStreamingStatistics();
  }

  @Override
  protected void doTearDown() throws Exception {
    super.doTearDown();
    statistics = null;
  }

  @Test
  public void openManyStreamsInForeachAndDiscard() throws Exception {
    String content = randomAlphanumeric(1024 * 1024);
    File file = new File(temporaryFolder.getRoot(), "file.txt");
    writeStringToFile(file, content);

    flowRunner("openManyStreamsInForeachAndDiscard").run();

    probe(TIMEOUT_MILLIS, POLL_DELAY_MILLIS, () -> {
      assertThat("Leaked Cursor Providers", statistics.getOpenCursorProvidersCount(), is(0));
      assertThat("Leaked Cursors", statistics.getOpenCursorsCount(), is(0));

      return true;
    });
  }

  // TODO MULE-17934 remove this
  @Override
  protected boolean isGracefulShutdown() {
    return true;
  }
}
