/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.extension.dsl;

import io.qameta.allure.Issue;
import org.junit.Rule;
import org.mule.tck.junit4.rule.SystemProperty;

import static org.mule.runtime.api.util.MuleSystemProperties.ENFORCE_EXPRESSION_VALIDATION_PROPERTY;

// TODO W-10883564 Remove this class and the config file
@Issue("W-10825584")
public class ConfigurationBasedElementModelFactoryWithoutExpressionValidationTestCase
    extends ConfigurationBasedElementModelFactoryTestCase {

  @Rule
  public SystemProperty systemProperty = new SystemProperty(ENFORCE_EXPRESSION_VALIDATION_PROPERTY, "false");

  @Override
  protected String getConfigFile() {
    return "integration-multi-config-dsl-app-without-expression-validation.xml";
  }

  @Override
  protected String getInputParameter() {
    return "#[{{'description' : payload}}]";
  }
}
