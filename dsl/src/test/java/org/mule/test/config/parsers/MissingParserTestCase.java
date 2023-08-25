/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.parsers;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;

import org.junit.Test;

public class MissingParserTestCase extends AbstractBadConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/test/config/parsers/missing-parser-test.xml";
  }

  @Test
  public void testHelpfulErrorMessage() throws Exception {
    expected.expectMessage(both(containsString("Invalid content was found starting with element"))
        .and(containsString("some-namespace")).and(containsString("missing")));

    parseConfig();
  }

}
