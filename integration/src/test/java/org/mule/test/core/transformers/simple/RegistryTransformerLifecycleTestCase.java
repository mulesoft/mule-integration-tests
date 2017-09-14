/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.InitialisationException;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.api.transformer.AbstractTransformer;
import org.mule.runtime.core.api.transformer.TransformerException;
import org.mule.test.AbstractIntegrationTestCase;

import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

/**
 * Highlights the issue: MULE-4599 where dispose cannot be called on a transformer since it is a prototype in Spring, so spring
 * does not manage the object.
 */
public class RegistryTransformerLifecycleTestCase extends AbstractIntegrationTestCase {

  @Override
  protected String getConfigFile() {
    return "simple-transformer-config.xml";
  }

  @Test
  public void testLifecycleInSpring() throws Exception {
    TransformerLifecycleTracker transformer = (TransformerLifecycleTracker) registry.lookupByName("lifecycle").get();
    assertNotNull(transformer);
    muleContext.dispose();
    assertInitialise(transformer);
  }

  @Test
  public void testLifecycleInFlowInSpring() throws Exception {
    Flow flow = registry.<Flow>lookupByName("flow").get();
    TransformerLifecycleTracker transformer = (TransformerLifecycleTracker) flow.getProcessors().get(0);

    assertNotNull(transformer);

    muleContext.dispose();
    assertLifecycle(transformer);
  }

  private void assertLifecycle(TransformerLifecycleTracker transformer) {
    assertEquals("[setProperty, initialise, dispose]", transformer.getTracker().toString());
  }

  private void assertInitialise(TransformerLifecycleTracker transformer) {
    assertEquals("[setProperty, initialise]", transformer.getTracker().toString());
  }

  public static class TransformerLifecycleTracker extends AbstractTransformer implements Disposable {

    private final List<String> tracker = new ArrayList<>();

    private String property;

    @Override
    protected Object doTransform(Object src, Charset encoding) throws TransformerException {
      tracker.add("doTransform");
      return null;
    }

    public String getProperty() {
      return property;
    }

    public void setProperty(String property) {
      tracker.add("setProperty");
    }

    public List<String> getTracker() {
      return tracker;
    }

    @Override
    public void initialise() throws InitialisationException {
      tracker.add("initialise");
    }

    @Override
    public void dispose() {
      tracker.add("dispose");
    }
  }
}
