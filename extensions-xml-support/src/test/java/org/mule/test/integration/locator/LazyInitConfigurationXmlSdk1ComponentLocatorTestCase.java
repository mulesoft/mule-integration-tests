/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.locator;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.XmlSdk.XML_SDK;

import org.mule.functional.junit4.MuleArtifactFunctionalTestCase;
import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.IntegrationTestCaseRunnerConfig;

import java.util.List;

import javax.inject.Inject;

import org.junit.Rule;
import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;

@Features({@Feature(XML_SDK), @Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationXmlSdk1ComponentLocatorTestCase extends MuleArtifactFunctionalTestCase
    implements IntegrationTestCaseRunnerConfig {

  @Rule
  public DynamicPort listenPort = new DynamicPort("http.listener.port");
  @Rule
  public DynamicPort proxyPort = new DynamicPort("http.proxy.port");

  @Rule
  public SystemProperty path = new SystemProperty("path", "path");


  private static final int TOTAL_NUMBER_OF_LOCATIONS = 33;

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "org/mule/test/integration/locator/module-with-config-oauth.xml",
        "org/mule/test/integration/locator/module-with-config-http-oauth-auth-code.xml",
        "org/mule/test/integration/locator/module-with-config-http-noconfig.xml"};
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
    assertThat(locator.findAllLocations(), hasSize(TOTAL_NUMBER_OF_LOCATIONS));

    List<String> allLocations = locator
        .findAllLocations()
        .stream()
        .map(ComponentLocation::getLocation)
        .collect(toList());
    assertThat(allLocations.toString(), allLocations,
               containsInAnyOrder("tokenManagerConfig-sample-config",
                                  "github-httpreq-config-sample-config",
                                  "github-httpreq-config-sample-config/connection",
                                  "github-httpreq-config-sample-config/connection/0/0",
                                  "get-channels/processors/0",
                                  "get-channels/processors/1",

                                  "sample-config",
                                  "GetChannels",
                                  "GetChannels/source",
                                  "GetChannels/source/0/0",
                                  "GetChannels/processors/0",

                                  "_defaultGlobalElements",
                                  "RequestWithNoConfig",
                                  "RequestWithNoConfig/processors/0",
                                  "localhost-config-module-using-http-noconfig-default-config-global-element-suffix",
                                  "localhost-config-module-using-http-noconfig-default-config-global-element-suffix/connection",
                                  "do-request/processors/0",
                                  "do-request/processors/1",

                                  "request-with-oauth-auth-code-config-scConfig",
                                  "request-with-oauth-auth-code-config-scConfig/connection",
                                  "request-with-oauth-auth-code-config-scConfig/connection/0/0",
                                  "request-with-oauth-auth-code/processors/0",
                                  "tokenManagerConfig-scConfig",
                                  "listenerConfigOac",
                                  "listenerConfigOac/connection",
                                  "listen",
                                  "listen/source",
                                  "listen/processors/0",
                                  "scConfig",
                                  "requestConfigOac",
                                  "requestConfigOac/connection",
                                  "request",
                                  "request/processors/0"));
    assertThat(locator.find(builder().globalName("myFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("anotherFlow").build()), is(empty()));
  }

  @Test
  public void lazyMuleContextSmartConnectorsWithConfig() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("GetChannels"));

    assertThat(locator.find(builderFromStringRepresentation("GetChannels").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("GetChannels/processors/0").build()), is(not(empty())));
  }

  @Test
  @Issue("MULE-18259")
  @Description("Default values for parameters declared in XML SDK configs were only working when XSD validations was enabled")
  public void lazyMuleContextSmartConnectorsWithConfigAndDefaultParameters() throws IllegalAccessException {
    lazyComponentInitializer.initializeComponents(componentLocation -> componentLocation.getLocation().equals("request"));

    assertThat(locator.find(builderFromStringRepresentation("request").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("request/processors/0").build()), is(not(empty())));
    assertThat(locator.find(builderFromStringRepresentation("listenerConfigOac").build()), is(not(empty())));
  }

  @Test
  @Issue("MULE-18197")
  @Description("Apps with Smart connectors wih default global elelements are properly initialized with LazyInit")
  public void xmlSdkOperationWithDefaultConfig() {
    lazyComponentInitializer.initializeComponent(builder().globalName("RequestWithNoConfig").build());
    assertThat(locator.find(builder().globalName("RequestWithNoConfig").build()), is(not(empty())));
  }

}
