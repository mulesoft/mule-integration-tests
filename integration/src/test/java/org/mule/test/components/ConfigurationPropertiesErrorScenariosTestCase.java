/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.junit.rules.ExpectedException.none;
import static org.mockito.Matchers.contains;
import static org.mule.tck.MuleTestUtils.testWithSystemProperty;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.COMPONENT_CONFIGURATION_ERROR_SCEANRIOS;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CONFIGURATION_PROPERTIES)
@Story(COMPONENT_CONFIGURATION_ERROR_SCEANRIOS)
public class ConfigurationPropertiesErrorScenariosTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = none();

  @Description("Validates the exception message when the configuration-properties element is pointing to a non existent file")
  @Test
  public void nonExistentFile() throws Exception {
    expectedException
        .expectMessage(contains("Couldn't find configuration properties file non-existent.properties neither on classpath or in file system"));
    new ApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/components/non-existent-configuration-properties-file.xml"})
        .build();
  }

  @Description("Validates the exception message when the configuration-properties element is pointing to a non existent file defined with a system property")
  @Test
  public void nonExistentFileDefinedWithSystemProperty() throws Exception {
    testWithSystemProperty("env", "no-env", () -> {
      expectedException
          .expectMessage("Couldn't find resource: no-env.properties");
      new ApplicationContextBuilder()
          .setApplicationResources(new String[] {"org/mule/test/components/customizable-configuration-properties-file.xml"})
          .build();
    });

  }

  @Description("Validates the exception message when the configuration-properties element points has a file attribute with a placeholder that could not be resolved")
  @Test
  public void fileReferenceWithNoValuePlaceholder() throws Exception {
    expectedException
        .expectMessage(contains("Couldn't find configuration property value for key ${env} from properties provider system properties provider"));
    new ApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/components/customizable-configuration-properties-file.xml"})
        .build();
  }

}

