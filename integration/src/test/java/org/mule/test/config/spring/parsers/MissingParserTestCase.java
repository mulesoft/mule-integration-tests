/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;

import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.Test;

public class MissingParserTestCase extends AbstractBadConfigTestCase implements IntegrationTestCaseRunnerConfig {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/missing-parser-test.xml";
  }

  @Test
  public void testHelpfulErrorMessage() throws Exception {
    expected.expectMessage(both(containsString("Invalid content was found starting with element"))
        .and(containsString("parsers-test")).and(containsString("missing")));

    parseConfig();
  }

}
