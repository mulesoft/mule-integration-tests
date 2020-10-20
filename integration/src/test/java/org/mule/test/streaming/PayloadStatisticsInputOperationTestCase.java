/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.streaming;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static org.apache.commons.lang3.RandomStringUtils.randomAlphanumeric;
import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_DISABLE_PAYLOAD_STATISTICS;
import static org.mule.runtime.api.util.MuleSystemProperties.MULE_ENABLE_STATISTICS;
import static org.mule.runtime.core.api.util.FileUtils.cleanDirectory;
import static org.mule.test.allure.AllureConstants.StreamingFeature.STREAMING;
import static org.mule.test.allure.AllureConstants.StreamingFeature.StreamingStory.STATISTICS;

import java.io.IOException;

import org.apache.activemq.util.ByteArrayInputStream;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mule.runtime.core.api.management.stats.PayloadStatistics;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(STREAMING)
@Story(STATISTICS)
public class PayloadStatisticsInputOperationTestCase extends AbstractIntegrationTestCase {

  public static final int BYTES_SIZE = 1343;

  @ClassRule
  public static TemporaryFolder temporaryFolder = new TemporaryFolder();

  @Rule
  public SystemProperty workingDirSysProp = new SystemProperty("workingDir", temporaryFolder.getRoot().getPath());

  @Rule
  public SystemProperty bytesSize = new SystemProperty("bytesSize", "" + BYTES_SIZE);

  @Rule
  public SystemProperty withStatistics = new SystemProperty(MULE_ENABLE_STATISTICS, "true");

  @Rule
  public SystemProperty withPayloadStatistics = new SystemProperty(MULE_DISABLE_PAYLOAD_STATISTICS, "false");

  @Override
  protected String getConfigFile() {
    return "org/mule/streaming/payload-statistics-input-operation-config.xml";
  }

  @Before
  public void before() throws IOException {
    cleanDirectory(temporaryFolder.getRoot());
  }

  @Test
  public void streamInputOperation() throws Exception {
    flowRunner("streamInputOperation").withPayload(randomAlphanumeric(BYTES_SIZE)).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("streamInputOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("file:write"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(0L));
    assertThat(fileListStatistics.getInputByteCount(), is(BYTES_SIZE * 1L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void listInputOperation() throws Exception {
    flowRunner("listInputOperation").withPayload(asList("Sentinel", "Nimrod", "Master Mold")).run();

    final PayloadStatistics shredIterator =
        muleContext.getStatistics().getPayloadStatistics("listInputOperation/processors/0");

    assertThat(shredIterator.getComponentIdentifier(), is("marvel:wolverine-shred"));

    assertThat(shredIterator.getInvocationCount(), is(1L));

    assertThat(shredIterator.getInputObjectCount(), is(3L));
    assertThat(shredIterator.getInputByteCount(), is(0L));
    assertThat(shredIterator.getOutputObjectCount(), is(0L));
    assertThat(shredIterator.getOutputByteCount(), is(0L));
  }

  @Test
  public void dslGroupInputOperation() throws Exception {
    flowRunner("dslGroupInputOperation").withPayload(new ByteArrayInputStream("waterWaterEverywhere".getBytes(UTF_8))).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("dslGroupInputOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:wolverine-chill-out"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(4L));
    assertThat(fileListStatistics.getInputByteCount(), is(20L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void groupInputOperation() throws Exception {
    flowRunner("groupInputOperation").withPayload(new ByteArrayInputStream("waterWaterEverywhere".getBytes(UTF_8))).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("groupInputOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:wolverine-chill-out-quick"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(4L));
    assertThat(fileListStatistics.getInputByteCount(), is(20L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

  @Test
  public void iteratorInputOperation() throws Exception {
    flowRunner("iteratorInputOperation").withPayload(asList("Poker Card", "Pool Stick").iterator()).run();

    final PayloadStatistics fileListStatistics =
        muleContext.getStatistics().getPayloadStatistics("iteratorInputOperation/processors/0");

    assertThat(fileListStatistics.getComponentIdentifier(), is("marvel:gambit-charge-items"));

    assertThat(fileListStatistics.getInvocationCount(), is(1L));

    assertThat(fileListStatistics.getInputObjectCount(), is(2L));
    assertThat(fileListStatistics.getInputByteCount(), is(0L));
    assertThat(fileListStatistics.getOutputObjectCount(), is(0L));
    assertThat(fileListStatistics.getOutputByteCount(), is(0L));
  }

}
