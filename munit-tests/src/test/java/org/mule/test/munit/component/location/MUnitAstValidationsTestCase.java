/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.munit.component.location;

import static java.util.Arrays.asList;
import static org.junit.rules.ExpectedException.none;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.runner.RunnerDelegateTo;

import java.util.Collection;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

@RunnerDelegateTo(Parameterized.class)
public class MUnitAstValidationsTestCase extends MuleArtifactFunctionalTestCase {

  @Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return asList(new Object[][] {
        {"munit-duplicate-before-suite.xml",
            "[org/mule/test/munit/component/validation/munit-duplicate-before-suite.xml:13; "
                + "org/mule/test/munit/component/validation/munit-duplicate-before-suite.xml:16]: "
                + "The configuration element 'munit:before-suite' can only appear once."},
        {"munit-duplicate-after-suite.xml",
            "[org/mule/test/munit/component/validation/munit-duplicate-after-suite.xml:17; "
                + "org/mule/test/munit/component/validation/munit-duplicate-after-suite.xml:20]: "
                + "The configuration element 'munit:after-suite' can only appear once."},
        {"munit-duplicate-before-test.xml",
            "[org/mule/test/munit/component/validation/munit-duplicate-before-test.xml:21; "
                + "org/mule/test/munit/component/validation/munit-duplicate-before-test.xml:24]: "
                + "The configuration element 'munit:before-test' can only appear once."},
        {"munit-duplicate-after-test.xml",
            "[org/mule/test/munit/component/validation/munit-duplicate-after-test.xml:25; "
                + "org/mule/test/munit/component/validation/munit-duplicate-after-test.xml:28]: "
                + "The configuration element 'munit:after-test' can only appear once."},
    });
  }

  @Rule
  public ExpectedException expectedException = none();

  @Rule
  public SystemProperty munitServerPort = new DynamicPort("munit.server.port");

  private final String configFile;
  private final String expectedMessage;

  public MUnitAstValidationsTestCase(String configFile, String expectedMessage) {
    this.configFile = configFile;
    this.expectedMessage = expectedMessage;
  }

  @Override
  protected String getConfigFile() {
    return "org/mule/test/munit/component/validation/" + configFile;
  }

  @Override
  protected void doSetUpBeforeMuleContextCreation() throws Exception {
    super.doSetUpBeforeMuleContextCreation();

    expectedException.expectMessage(expectedMessage);
  }

  @Test
  public void duplicateBeforeSuite() throws Exception {
    // Nothing to do here, just the set up of the test is enough
  }

}
