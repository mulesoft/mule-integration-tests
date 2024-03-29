/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.containsString;

import org.mule.runtime.core.api.MuleContext;
import org.mule.test.AbstractIntegrationTestCase;

import org.junit.Rule;
import org.junit.rules.ExpectedException;

/**
 * A stripped-down version of FunctionalTestCase that allows us to test the parsing of a bad configuration.
 */
public abstract class AbstractBadConfigTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  @Override
  protected boolean doTestClassInjection() {
    return false;
  }

  @Override
  protected MuleContext createMuleContext() throws Exception {
    return null;
  }

  public void assertErrorContains(String phrase) throws Exception {
    expected.expectMessage(containsString(phrase));

    parseConfig();
  }

  protected void parseConfig() throws Exception {
    super.createMuleContext();
  }

}
