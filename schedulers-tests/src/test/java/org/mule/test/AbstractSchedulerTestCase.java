/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.test.allure.AllureConstants.SchedulerFeature.SCHEDULER;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;

import javax.inject.Inject;

import io.qameta.allure.Feature;

@Feature(SCHEDULER)
public class AbstractSchedulerTestCase extends MuleArtifactFunctionalTestCase {

  @Inject
  protected Registry registry;

  @Inject
  protected ConfigurationComponentLocator locator;

  @Override
  protected boolean doTestClassInjection() {
    return true;
  }

}
