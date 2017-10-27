/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers;

import static java.lang.String.format;

import java.util.Arrays;

import org.junit.Test;

public class MultipleConfigSingletonsTestCase extends AbstractBadConfigTestCase {

  private static final String REPEATED_ELEMENT_NAME = "configuration";
  private static final String FIRST_CONFIG = "org/mule/config/spring/parsers/multiple-config-singletons-1.xml";
  private static final String SECOND_CONFIG = "org/mule/config/spring/parsers/multiple-config-singletons-2.xml";

  @Override
  protected String[] getConfigFiles() {
    return Arrays.asList(FIRST_CONFIG, SECOND_CONFIG).toArray(new String[0]);
  }

  @Test
  public void testDuplicatedSingletonElementError() throws Exception {
    assertErrorContains(format("The configuration element [%s] can only appear once, but was present in both [%s:8] and [%s:12]",
                               REPEATED_ELEMENT_NAME, FIRST_CONFIG, SECOND_CONFIG));
  }

}
