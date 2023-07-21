/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.issues;

import static org.junit.Assert.assertEquals;
import static org.mule.functional.api.component.FunctionalTestProcessor.addLifecycleCallback;
import static org.mule.functional.api.component.FunctionalTestProcessor.removeLifecycleCallback;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.api.lifecycle.Disposable;
import org.mule.runtime.api.lifecycle.Initialisable;
import org.mule.runtime.api.lifecycle.Startable;
import org.mule.runtime.api.lifecycle.Stoppable;
import org.mule.runtime.core.api.MuleContext;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

public class AsyncComponentLifecycleIssue5649TestCase extends AbstractIntegrationTestCase
    implements FunctionalTestProcessor.LifecycleCallback {

  List<String> componentPhases = new ArrayList<>();

  @Override
  protected MuleContext createMuleContext() throws Exception {
    componentPhases.clear();
    addLifecycleCallback(this);
    return super.createMuleContext();
  }

  @Override
  protected void doTearDown() throws Exception {
    removeLifecycleCallback(this);
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/issues/async-component-lifecycle-5649.xml";
  }

  @Test
  public void testConfig() throws Exception {
    muleContext.stop();
    muleContext.dispose();
    removeLifecycleCallback(this);
    System.out.println(componentPhases);
    assertEquals(4, componentPhases.size());
    assertEquals(Initialisable.PHASE_NAME, componentPhases.get(0));
    assertEquals(Startable.PHASE_NAME, componentPhases.get(1));
    assertEquals(Stoppable.PHASE_NAME, componentPhases.get(2));
    assertEquals(Disposable.PHASE_NAME, componentPhases.get(3));
  }

  @Override
  public void onTransition(String name, String newPhase) {
    if ("async".equals(name)) {
      componentPhases.add(newPhase);
    }
  }
}
