/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.shutdown;

import org.mule.runtime.api.component.Component;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.runtime.api.exception.DefaultMuleException;
import org.mule.runtime.core.api.event.CoreEvent;
import org.mule.runtime.api.exception.MuleException;
import org.mule.runtime.core.api.processor.Processor;
import org.mule.runtime.api.util.concurrent.Latch;
import org.mule.tck.junit4.rule.DynamicPort;

import java.util.Map;

import javax.xml.namespace.QName;

import org.junit.Before;
import org.junit.Rule;

public abstract class AbstractShutdownTimeoutRequestResponseTestCase extends AbstractIntegrationTestCase {

  protected static int WAIT_TIME = 2000;
  protected static Latch waitLatch;

  @Rule
  public DynamicPort httpPort = new DynamicPort("httpPort");

  @Before
  public void setUpWaitLatch() throws Exception {
    waitLatch = new Latch();
  }

  private static class BlockMessageProcessor implements Processor,Component {

    @Override
    public CoreEvent process(CoreEvent event) throws MuleException {
      waitLatch.release();

      try {
        Thread.sleep(WAIT_TIME);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
        throw new DefaultMuleException(e);
      }

      return event;
    }

    @Override
    public Object getAnnotation(QName name) {
      return null;
    }

    @Override
    public Map<QName, Object> getAnnotations() {
      return null;
    }

    @Override
    public void setAnnotations(Map<QName, Object> annotations) {

    }

    @Override
    public ComponentLocation getLocation() {
      return null;
    }

    @Override
    public Location getRootContainerLocation() {
      return null;
    }
  }
}
