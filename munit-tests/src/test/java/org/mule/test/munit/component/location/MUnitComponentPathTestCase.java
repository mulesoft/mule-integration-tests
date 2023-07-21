/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.munit.component.location;

import static org.hamcrest.core.IsNull.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.MUnitComponentLocatorStory.MUNIT_COMPONENT_LOCATION;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(MUNIT_COMPONENT_LOCATION)
public class MUnitComponentPathTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty munitServerPort = new DynamicPort("munit.server.port");

  @Override
  protected String getConfigFile() {
    return "org/mule/test/munit/component/location/munit-component-path-test-flow.xml";
  }

  @Test
  public void beforeSuiteComponentLocations() throws Exception {
    assertThat(locator.find(builderFromStringRepresentation("beforeSuite").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("beforeSuite/processors/0").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("beforeSuite/processors/1").build()).get(),
               notNullValue());
  }

  @Test
  public void afterSuiteComponentLocations() throws Exception {
    assertThat(locator.find(builderFromStringRepresentation("afterSuite").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("afterSuite/processors/0").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("afterSuite/processors/0/route/0/processors/0").build())
        .get(),
               notNullValue());
  }

  @Test
  public void beforeTestComponentLocations() throws Exception {
    assertThat(locator.find(builderFromStringRepresentation("beforeTest").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("beforeTest/processors/0").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("beforeTest/processors/0/processors/0").build()).get(),
               notNullValue());
    assertThat(locator
        .find(builderFromStringRepresentation("beforeTest/processors/0/processors/0/route/0/processors/0").build()).get(),
               notNullValue());
    assertThat(locator
        .find(builderFromStringRepresentation("beforeTest/processors/0/processors/0/route/1/processors/0").build()).get(),
               notNullValue());
  }

  @Test
  public void afterTestComponentLocations() throws Exception {
    assertThat(locator.find(builderFromStringRepresentation("afterTest").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("afterTest/processors/0").build()).get(),
               notNullValue());
  }

  @Test
  public void testComponentLocations() throws Exception {
    assertThat(locator.find(builderFromStringRepresentation("test").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/0").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/0/processors/0").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/0/processors/0/processors/0").build()).get(),
               notNullValue());
    assertThat(locator
        .find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0/processors/0").build())
        .get(), notNullValue());
    assertThat(locator
        .find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0/processors/0/processors/0")
            .build())
        .get(), notNullValue());
    assertThat(locator
        .find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0").build())
        .get(), notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/0/processors/0/errorHandler").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/0/processors/0/errorHandler/0").build()).get(),
               notNullValue());

    assertThat(locator.find(builderFromStringRepresentation("test/route/1").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/1/processors/0").build()).get(),
               notNullValue());

    assertThat(locator.find(builderFromStringRepresentation("test/route/2").build()).get(),
               notNullValue());
    assertThat(locator.find(builderFromStringRepresentation("test/route/2/processors/0").build()).get(),
               notNullValue());
  }

}
