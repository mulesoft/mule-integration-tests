/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;

import org.junit.Test;

public class ConstrainedConnectorExceptionStrategyMule2126TestCase extends AbstractBadConfigTestCase {

  @Override
  protected String getConfigFile() {
    return "org/mule/config/spring/parsers/constrained-connector-exception-strategy-mule-2126-test.xml";
  }

  @Test
  public void testError() throws Exception {
    expected.expectMessage(both(containsString("Invalid content was found starting with element"))
        .and(containsString("default-connector-exception-strategy")).and(containsString("One of"))
        .and(containsString("is expected")));

    parseConfig();
  }

}
