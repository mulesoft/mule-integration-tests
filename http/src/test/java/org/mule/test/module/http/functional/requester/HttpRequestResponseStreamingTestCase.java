/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.http.functional.requester;

import static org.mule.runtime.api.message.Message.of;
import static org.mule.test.allure.AllureConstants.HttpFeature.HTTP_EXTENSION;
import static org.mule.test.allure.AllureConstants.HttpFeature.HttpStory.STREAMING;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.Event;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;

import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Before;
import org.junit.Test;
import ru.yandex.qatools.allure.annotations.Features;
import ru.yandex.qatools.allure.annotations.Stories;

@Features(HTTP_EXTENSION)
@Stories(STREAMING)
public class HttpRequestResponseStreamingTestCase extends AbstractHttpRequestTestCase {

  private static final int POLL_DELAY = 1000;

  protected static AtomicBoolean stop;
  protected static AtomicBoolean executed;

  public final PollingProber pollingProber = new PollingProber(RECEIVE_TIMEOUT, POLL_DELAY);
  private Probe processorExecuted = new Probe() {

    @Override
    public boolean isSatisfied() {
      return executed.get();
    }

    @Override
    public String describeFailure() {
      return "Processor should have executed at this point.";
    }

  };
  private Probe processorNotExecuted = new Probe() {

    @Override
    public boolean isSatisfied() {
      return !executed.get();
    }

    @Override
    public String describeFailure() {
      return "Processor should not have executed at this point.";
    }

  };

  @Override
  protected String getConfigFile() {
    return "http-request-response-streaming-config.xml";
  }

  @Before
  public void setUp() {
    stop = new AtomicBoolean(false);
    executed = new AtomicBoolean(false);
  }

  @Test
  public void executionIsExpeditedWhenStreaming() throws Exception {
    flowRunner("streamingClient").dispatchAsync();
    pollingProber.check(processorExecuted);
    stop.set(true);
  }

  @Test
  public void executionHangsWhenNotStreaming() throws Exception {
    flowRunner("noStreamingClient").dispatchAsync();
    pollingProber.check(processorNotExecuted);
    stop.set(true);
    pollingProber.check(processorExecuted);
  }

  protected static class StatusProcessor implements Processor {


    @Override
    public Event process(Event event) throws MuleException {
      executed.set(true);
      return event;
    }

  }

  protected static class StoppableInputStreamProcessor implements Processor {

    @Override
    public Event process(Event event) throws MuleException {
      InputStream inputStream = new InputStream() {

        @Override
        public int read() throws IOException {
          if (stop.get()) {
            return -1;
          } else {
            return 1;
          }
        }
      };
      return Event.builder(event).message(of(inputStream)).build();
    }

  }
}
