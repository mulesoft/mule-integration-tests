/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.core.api.config.MuleProperties.OBJECT_SECURITY_MANAGER;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.extension.spring.api.SpringConfig;
import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.artifact.Registry;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.security.SecurityManager;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.List;

import javax.inject.Inject;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitSecurityConfigurationComponentLocatorTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");

  @Inject
  private Registry registry;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "org/mule/test/integration/locator/component-locator-spring-config.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Override
  public boolean disableXmlValidations() {
    return true;
  }

  @Description("Lazy init should not create components until an operation is done")
  @Test
  public void lazyInitCalculatesLocations() {
    List<String> allLocations = locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList());
    assertThat(allLocations.toString(), allLocations,
               containsInAnyOrder("springConfig",
                                  "null",
                                  "null/0",
                                  "securityManager2",
                                  "securityManager2/0",
                                  "listenerConfig",
                                  "listenerConfig/connection",
                                  "SecureUMO",
                                  "SecureUMO/source",
                                  "SecureUMO/processors/0",
                                  "SecureUMO/processors/1",
                                  "SecureUMO2",
                                  "SecureUMO2/source",
                                  "SecureUMO2/processors/0",
                                  "SecureUMO2/processors/0/0/0",
                                  "SecureUMO2/processors/1"));
  }

  @Description("Lazy init should create spring components without dependencies")
  @Test
  public void lazyMuleContextInitializesSpringConfig() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("myFlow"));

    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));

    assertThat(registry.lookupByName("springConfig").isPresent(), is(true));

    SpringConfig springConfig = (SpringConfig) registry.lookupByName("springConfig").get();
    Object applicationContext = FieldUtils.readField(springConfig, "applicationContext", true);
    assertThat("springConfig was not configured", applicationContext, notNullValue());

    assertThat(springConfig.getObject("child1").isPresent(), is(true));
  }

  @Description("Lazy init should create spring components without dependencies")
  @Test
  public void lazyMuleContextShouldNotFailWhenTryingToInitializeGlobalProperty() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("some.property"));
    assertThat(locator.find(builderFromStringRepresentation("some.property").build()), is(empty()));
  }

  @Description("Lazy init should create spring security manager without dependencies")
  @Test
  public void lazyMuleContextInitializesSpringSecurityManager() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("SecureUMO"));

    assertThat(locator.find(builderFromStringRepresentation("listenerConfig").build()), is(not(empty())));
    assertThat(locator.find(Location.builderFromStringRepresentation("listenerConfig/connection").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO/source").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO/processors/1").build()), is(not(empty())));

    assertThat(registry.lookupByName(OBJECT_SECURITY_MANAGER).isPresent(), is(true));

    SecurityManager securityManager = (SecurityManager) registry.lookupByName(OBJECT_SECURITY_MANAGER).get();
    assertThat("spring security provider was not registered", securityManager.getProvider("memory-dao"), notNullValue());
  }

  @Description("Lazy init should create spring security manager without dependencies")
  @Test
  public void lazyMuleContextInitializesNamedSpringSecurityManager() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("SecureUMO2"));

    assertThat(locator.find(builderFromStringRepresentation("listenerConfig").build()), is(not(empty())));
    assertThat(locator.find(Location.builderFromStringRepresentation("listenerConfig/connection").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO2/source").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO2/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("SecureUMO2/processors/1").build()), is(not(empty())));

    assertThat(registry.lookupByName(OBJECT_SECURITY_MANAGER).isPresent(), is(true));

    SecurityManager securityManager = (SecurityManager) registry.lookupByName("securityManager2").get();
    assertThat("spring security provider was not registered", securityManager.getProvider("memory-dao2"), notNullValue());
  }

  @Description("Spring component should be created each time as the rest")
  @Test
  public void lazyMuleContextSpringConfigRebuilt() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("SecureUMO"));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));
    Object firstObj = locator.find(builderFromStringRepresentation("springConfig").build()).get();

    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("SecureUMO2"));
    assertThat(locator.find(builderFromStringRepresentation("springConfig").build()), is(not(empty())));
    Object secondObj = locator.find(builderFromStringRepresentation("springConfig").build()).get();
    Object secondAppContext = FieldUtils.readField(secondObj, "applicationContext", true);
    assertThat("springConfig was not configured", secondAppContext, notNullValue());

    assertThat(firstObj, not(sameInstance(secondObj)));
  }

}
