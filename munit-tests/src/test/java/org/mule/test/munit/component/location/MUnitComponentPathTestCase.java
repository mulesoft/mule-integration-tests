/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.munit.component.location;

import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.tck.junit4.matcher.IsEmptyOptional.empty;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.MUnitComponentLocatorStory.MUNIT_COMPONENT_LOCATION;

import static java.util.stream.Collectors.toCollection;

import static org.hamcrest.CoreMatchers.not;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;

import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(MUNIT_COMPONENT_LOCATION)
public class MUnitComponentPathTestCase extends MuleArtifactFunctionalTestCase {

  @Rule
  public SystemProperty munitServerPort = new DynamicPort("munit.server.port");

  private Set<String> locations;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/munit/component/location/munit-component-path-test-flow.xml";
  }

  @Before
  public void before() {
    locations = locator.findAllLocations().stream()
        .map(loc -> loc.getLocation())
        .collect(toCollection(TreeSet::new));
  }

  @Test
  public void beforeSuiteComponentLocations() throws Exception {
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeSuite").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeSuite/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeSuite/processors/1").build()),
               not(empty()));
  }

  @Test
  public void afterSuiteComponentLocations() throws Exception {
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("afterSuite").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("afterSuite/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("afterSuite/processors/0/route/0/processors/0").build()),
               not(empty()));
  }

  @Test
  public void beforeTestComponentLocations() throws Exception {
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeTest").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeTest/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeTest/processors/0/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeTest/processors/0/processors/0/route/0/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("beforeTest/processors/0/processors/0/route/1/processors/0").build()),
               not(empty()));
  }

  @Test
  public void afterTestComponentLocations() throws Exception {
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("afterTest").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("afterTest/processors/0").build()),
               not(empty()));
  }

  @Test
  public void testComponentLocations() throws Exception {
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0/processors/0/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator
                   .find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0/processors/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator
                   .find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0/processors/0/processors/0")
                       .build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0/processors/0/processors/0/route/0").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0/processors/0/errorHandler").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/0/processors/0/errorHandler/0").build()),
               not(empty()));

    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/1").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/1/processors/0").build()),
               not(empty()));

    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/2").build()),
               not(empty()));
    assertThat("locations: " + locations,
               locator.find(builderFromStringRepresentation("test/route/2/processors/0").build()),
               not(empty()));
  }

}
