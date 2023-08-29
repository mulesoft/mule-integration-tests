/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SOURCES;
import static org.mule.test.allure.AllureConstants.SourcesFeature.SourcesStories.WATERMARK;

import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.api.message.Message;
import org.mule.runtime.api.metadata.TypedValue;
import org.mule.runtime.api.store.ObjectStore;
import org.mule.runtime.api.store.ObjectStoreManager;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.test.AbstractIntegrationTestCase;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(SOURCES)
@Story(WATERMARK)
public class WatermarkTestCase extends AbstractIntegrationTestCase {

  private static final String LAST_WATERMARK_KEY = "lastWatermark";
  private static final String WATERMARK_KEY = "watermark";

  public static class DataSource implements Processor {

    private static int ID = 0;

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      final int top = 5;
      List<Integer> numbers = new ArrayList<>(top);
      for (int i = ID; i < top + ID; i++) {
        numbers.add(i);
      }

      ID += top;

      return CoreEvent.builder(event)
          .message(Message.builder(event.getMessage())
              .payload(TypedValue.of(numbers))
              .build())
          .build();
    }
  }

  private ObjectStoreManager objectStoreManager;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/watermark-config.xml";
  }

  @Override
  protected void doSetUp() throws Exception {
    reset();
    objectStoreManager = registry.<ObjectStoreManager>lookupByType(ObjectStoreManager.class).get();
  }

  @Override
  protected void doTearDown() throws Exception {
    reset();
  }

  @Test
  public void watermark() throws Exception {
    ObjectStore<Integer> os = objectStoreManager.getObjectStore("watermarkStore");
    assertThat(os.contains(LAST_WATERMARK_KEY), is(false));
    assertThat(os.contains(WATERMARK_KEY), is(false));

    runWatermarkFlow();
    assertThat(retrieve(os, LAST_WATERMARK_KEY), is(0));
    assertThat(retrieve(os, WATERMARK_KEY), is(4));

    runWatermarkFlow();
    assertThat(retrieve(os, LAST_WATERMARK_KEY), is(4));
    assertThat(retrieve(os, WATERMARK_KEY), is(9));

  }

  private void runWatermarkFlow() throws Exception {
    flowRunner("watermark").run();
  }

  private <T extends Serializable> T retrieve(ObjectStore<T> os, String key) throws Exception {
    TypedValue<T> typedValue = (TypedValue<T>) os.retrieve(key);
    return typedValue.getValue();
  }

  private void reset() {
    DataSource.ID = 0;
  }
}
