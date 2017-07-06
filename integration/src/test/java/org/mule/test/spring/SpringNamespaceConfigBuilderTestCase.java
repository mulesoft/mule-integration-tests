/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.spring;

import org.mule.functional.AbstractConfigBuilderTestCase;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import org.junit.Rule;

public class SpringNamespaceConfigBuilderTestCase extends AbstractConfigBuilderTestCase implements
    IntegrationTestCaseRunnerConfig {

  @Rule
  public SystemProperty springConfigFiles =
      new SystemProperty(SPRING_CONFIG_FILES_PROPERTIES, "org/mule/test/spring/config1/test-xml-mule2-config-beans.xml");

  public SpringNamespaceConfigBuilderTestCase() {
    super(false);
    setDisposeContextPerClass(true);
  }

  @Override
  public String[] getConfigFiles() {
    return new String[] {"org/mule/test/spring/config1/test-xml-mule2-config.xml",
        "org/mule/test/spring/config1/test-xml-mule2-config-split.xml",
        MULE_SPRING_CONFIG_FILE};
  }

}
