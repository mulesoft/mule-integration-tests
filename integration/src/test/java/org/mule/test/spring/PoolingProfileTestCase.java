/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_ACTIVE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_IDLE;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_MAX_POOL_WAIT;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_EXHAUSTED_ACTION;
import static org.mule.runtime.api.config.PoolingProfile.DEFAULT_POOL_INITIALISATION_POLICY;
import static org.mule.runtime.api.config.PoolingProfile.INITIALISE_ALL;
import static org.mule.runtime.api.config.PoolingProfile.INITIALISE_NONE;
import static org.mule.runtime.api.config.PoolingProfile.INITIALISE_ONE;
import static org.mule.runtime.api.config.PoolingProfile.WHEN_EXHAUSTED_FAIL;
import static org.mule.runtime.api.config.PoolingProfile.WHEN_EXHAUSTED_GROW;
import static org.mule.runtime.api.config.PoolingProfile.WHEN_EXHAUSTED_WAIT;

import org.mule.functional.api.component.FunctionalTestProcessor;
import org.mule.runtime.api.config.PoolingProfile;
import org.mule.runtime.core.api.construct.Flow;
import org.mule.runtime.core.component.PooledJavaComponent;
import org.mule.tck.probe.PollingProber;
import org.mule.tck.probe.Probe;
import org.mule.tck.probe.Prober;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Ignore;
import org.junit.Test;

@Ignore("MULE-11686")
public class PoolingProfileTestCase extends AbstractIntegrationTestCase {

  private static boolean evicted;

  @Override
  protected String getConfigFile() {
    return "pooling-profile-test-flow.xml";
  }

  @Test
  public void testDefault() {
    doTest("default", DEFAULT_POOL_EXHAUSTED_ACTION, DEFAULT_POOL_INITIALISATION_POLICY, DEFAULT_MAX_POOL_ACTIVE,
           DEFAULT_MAX_POOL_IDLE, DEFAULT_MAX_POOL_WAIT);
  }

  @Test
  public void testFailAll() {
    doTest("fail_all", WHEN_EXHAUSTED_FAIL, INITIALISE_ALL, 1, 2, 3);
  }

  @Test
  public void testGrowOne() {
    doTest("grow_one", WHEN_EXHAUSTED_GROW, INITIALISE_ONE, 2, 3, 4);
  }

  @Test
  public void testWaitNone() {
    doTest("wait_none", WHEN_EXHAUSTED_WAIT, INITIALISE_NONE, 3, 4, 5);
  }

  @Test
  public void testEvictOne() {
    doTest("evict_one", WHEN_EXHAUSTED_WAIT, INITIALISE_ALL, 1, 1, 0);


    Prober prober = new PollingProber(5000, 50);
    prober.check(new Probe() {

      @Override
      public boolean isSatisfied() {
        return evicted;
      }

      @Override
      public String describeFailure() {
        return "Pooled component was not evicted";
      }
    });
  }

  protected void doTest(String serviceFlow, int exhausted, int initialisation, int active, int idle, long wait) {
    Object o = muleContext.getRegistry().lookupObject(serviceFlow);
    assertNotNull(serviceFlow, o);


    assertTrue(((Flow) o).getProcessors().get(0) instanceof PooledJavaComponent);
    PooledJavaComponent pjc = (PooledJavaComponent) ((Flow) o).getProcessors().get(0);

    PoolingProfile profile = pjc.getPoolingProfile();
    assertNotNull(profile);
    assertEquals("exhausted:", exhausted, profile.getExhaustedAction());
    assertEquals("initialisation:", initialisation, profile.getInitialisationPolicy());
    assertEquals("active:", active, profile.getMaxActive());
    assertEquals("idle:", idle, profile.getMaxIdle());
    assertEquals("wait:", wait, profile.getMaxWait());
  }

  public static class EvictablePooledComponent extends FunctionalTestProcessor {

    @Override
    public void dispose() {
      super.dispose();
      evicted = true;
    }
  }
}
