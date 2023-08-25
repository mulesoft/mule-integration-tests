/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
