/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.stream.Collectors.toList;
import static java.util.Optional.empty;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.mule.runtime.api.component.location.Location.builder;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Ignore;
import org.junit.Test;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.config.ConfigurationBuilder;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import java.util.List;

import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.runtime.config.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationComponentLocatorTestCase extends AbstractIntegrationTestCase {

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    // TODO (MULE-13666): add config "org/mule/test/integration/locator/component-locator-spring-config.xml"
    // Cannot add it until MULE-13666 is not fixed, since lazy init will throw NPE
    return new String[] {"org/mule/test/integration/locator/component-locator-config.xml",
        "org/mule/test/integration/locator/component-locator-levels-config.xml"};
  }

  @Override
  protected ConfigurationBuilder getBuilder() throws Exception {
    final ConfigurationBuilder configurationBuilder = createConfigurationBuilder(getConfigFiles(), true);
    configureSpringXmlConfigurationBuilder(configurationBuilder);
    return configurationBuilder;
  }

  @Description("Lazy init should not create components until an operation is done")
  @Test
  public void lazyInitDoesNotCalculateLocations() {
    assertThat(muleContext.getConfigurationComponentLocator().findAllLocations(), hasSize(0));
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build()), is(empty()));
  }

  @Description("Lazy init should create components when operation is done")
  @Test
  public void lazyMuleContextInitializesLocation() {
    lazyComponentInitializer.initializeComponent(builder().globalName("myFlow").build());
    List<ComponentLocation> componentLocs = muleContext.getConfigurationComponentLocator().findAllLocations();
    List<String> allComponentPaths = componentLocs.stream().map(ComponentLocation::getLocation).collect(toList());
    assertThat(allComponentPaths, containsInAnyOrder(
                                                     "myFlow",
                                                     "myFlow/source",
                                                     "myFlow/source/0",
                                                     "myFlow/source/0/0",
                                                     "myFlow/processors/0",
                                                     "myFlow/processors/1",
                                                     "myFlow/processors/2",
                                                     "myFlow/processors/2/processors/0",
                                                     "myFlow/processors/2/processors/1"));
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build()), is(not(empty())));
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("anotherFlow").build()), is(empty()));
  }

  @Description("Lazy init should refresh the ConfigurationComponentLocator when initialize is done")
  @Test
  public void lazyMuleContextRefreshesConfigurationComponentLoader() {
    lazyComponentInitializer.initializeComponent(builder().globalName("myFlow").build());
    assertThat(muleContext.getConfigurationComponentLocator().findAllLocations(), hasSize(9));

    lazyComponentInitializer.initializeComponent(builder().globalName("anotherFlow").build());
    List<ComponentLocation> componentLocs = muleContext.getConfigurationComponentLocator().findAllLocations();
    List<String> allComponentPaths = componentLocs.stream().map(ComponentLocation::getLocation).collect(toList());
    assertThat(allComponentPaths, containsInAnyOrder(
                                                     "anotherFlow",
                                                     "anotherFlow/source",
                                                     "anotherFlow/source/0",
                                                     "anotherFlow/source/0/0",
                                                     "anotherFlow/processors/0"));
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("myFlow").build()), is(empty()));
    assertThat(muleContext.getConfigurationComponentLocator().find(builder().globalName("anotherFlow").build()),
               is(not(empty())));
  }

  @Test
  public void lazyMuleContextWithDeeperLevelConfig() {
    lazyComponentInitializer.initializeComponent(builder().globalName("flowLvl0").build());
    List<ComponentLocation> componentLocs = muleContext.getConfigurationComponentLocator().findAllLocations();
    List<String> allComponentPaths = componentLocs.stream().map(ComponentLocation::getLocation).collect(toList());

    assertThat(allComponentPaths, containsInAnyOrder(
                                                     "flowLvl0",
                                                     "flowLvl0/processors/0",
                                                     "flowLvl1",
                                                     "flowLvl1/processors/0",
                                                     "flowLvl2",
                                                     "flowLvl2/processors/0",
                                                     "flowLvl2/processors/0/0",
                                                     "flowLvl2/processors/1",
                                                     "dbConfig",
                                                     "dbConfig/0",
                                                     "requestConfig",
                                                     "requestConfig/0",
                                                     "tlsContextRef",
                                                     "tlsContextRef/0"));
  }

}
