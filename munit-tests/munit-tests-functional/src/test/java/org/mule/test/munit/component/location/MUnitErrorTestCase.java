/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.munit.component.location;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;

import org.junit.Test;

import io.qameta.allure.Issue;

public class MUnitErrorTestCase extends MuleArtifactFunctionalTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/munit/component/error/munit-expected-error.xml";
  }

  @Test
  @Issue("MULE-17709")
  public void testWithExpectedError() throws Exception {
    // Nothing to do here, just the set up of the test is enough
  }

}
