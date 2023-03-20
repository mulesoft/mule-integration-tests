/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.locator;

import static org.mule.runtime.api.component.ComponentIdentifier.buildFromStringRepresentation;
import static org.mule.runtime.api.component.location.Location.builderFromStringRepresentation;
import static org.mule.runtime.api.dsl.DslResolvingContext.getDefault;
import static org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest.builder;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.TYPE_PROPERTY_NAME;
import static org.mule.runtime.module.extension.internal.loader.java.AbstractJavaExtensionModelLoader.VERSION;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;

import static java.lang.String.format;
import static java.util.Collections.emptySet;
import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;

import static com.google.common.collect.ImmutableSet.of;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;

import org.mule.functional.junit4.DomainFunctionalTestCase;
import org.mule.runtime.api.component.location.ConfigurationComponentLocator;
import org.mule.runtime.api.meta.NamedObject;
import org.mule.runtime.api.meta.model.ExtensionModel;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.runtime.core.api.extension.MuleExtensionModelProvider;
import org.mule.runtime.extension.api.loader.ExtensionModelLoadingRequest;
import org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader;
import org.mule.test.petstore.extension.PetStoreConnector;

import java.util.List;
import java.util.Set;

import io.qameta.allure.Feature;
import io.qameta.allure.Features;
import io.qameta.allure.Story;
import org.junit.Before;
import org.junit.Test;

@Features({@Feature(LAZY_INITIALIZATION), @Feature(CONFIGURATION_COMPONENT_LOCATOR)})
@Story(SEARCH_CONFIGURATION)
public class LazyInitComponentWithParentArtifactTestCase extends DomainFunctionalTestCase {

  private static final String APP_NAME = "app-depending-on-parent-config";

  private LazyComponentInitializer appLazyComponentInitializer;
  private ConfigurationComponentLocator appLocator;
  private ConfigurationComponentLocator domainLocator;

  @Override
  protected boolean enableLazyInit() {
    return true;
  }

  @Override
  protected boolean disableXmlValidations() {
    return true;
  }

  @Override
  public ApplicationConfig[] getConfigResources() {
    return new ApplicationConfig[] {
        new ApplicationConfig(APP_NAME,
                              "org/mule/test/integration/locator/component-locator-depending-on-parent-config.xml")
    };
  }

  @Override
  protected String[] getDomainConfigs() {
    return new String[] {"org/mule/test/integration/locator/domain-with-http-config.xml"};
  }

  @Override
  protected Set<ExtensionModel> getExtensionModels() {
    return of(MuleExtensionModelProvider.getExtensionModel(),
              loadJavaSdkExtension(PetStoreConnector.class, this.getClass().getClassLoader(), emptySet()));
  }

  @Before
  public void grabObjectsFromArtifactsInfrastructure() {
    appLazyComponentInitializer = getLazyComponentInitializerForApp(APP_NAME);
    appLocator = getLocatorForApp(APP_NAME);
    domainLocator = getLocatorForDomain();
  }

  @Test
  public void whenInitializingComponentsDependingOnComponentInParentArtifactThenComponentsThatAreNotReferencedAreNotInitialized() {
    // Initializes a flow with a listener depending on a config from the domain
    appLazyComponentInitializer.initializeComponent(builderFromStringRepresentation("flowDependingOnConfigFromDomain").build());

    // This config from the domain is not referenced anywhere from the full
    assertLocationsNotInitialized(domainLocator, "anotherConfigThatShouldNotBeInitialized");

    // We are not currently guaranteeing that other components in the domain that are referenced from the full application are not
    // initialized. We are explicitly not asserting that here. We might eventually want to add that as an enhancement.
    // assertLocationsNotInitialized(domainLocator, "configInDomain");
  }

  @Test
  public void whenInitializingComponentsDependingOnComponentInParentArtifactThenBothAreInitialized() {
    // Initializes a flow with a listener depending on a config from the domain
    appLazyComponentInitializer.initializeComponent(builderFromStringRepresentation("flowDependingOnConfigFromDomain").build());

    // Components in the app
    assertFlowsInitialized(appLocator, "flowDependingOnConfigFromDomain");
    assertLocationsInitialized(appLocator, "flowDependingOnConfigFromDomain", "flowDependingOnConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "anotherFlowThatShouldNotBeInitialized");

    // Components in the domain
    assertLocationsInitialized(domainLocator, "configInDomain");
    assertLocationsNotInitialized(domainLocator, "anotherConfigThatShouldNotBeInitialized");
  }

  @Test
  public void whenInitializingTwoComponentsDependingOnComponentInParentArtifactThenSecondAreInitialized() {
    // Initializes a flow with a listener depending on a config from the domain
    appLazyComponentInitializer
        .initializeComponent(builderFromStringRepresentation("anotherFlowDependingOnAnotherConfigFromDomain").build());

    assertLocationsInitialized(appLocator, "anotherFlowDependingOnAnotherConfigFromDomain",
                               "anotherFlowDependingOnAnotherConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "flowDependingOnConfigFromDomain");
    assertLocationsInitialized(domainLocator, "anotherConfigInDomain");

    // Initializes another flow with a listener depending on another config from the domain
    appLazyComponentInitializer.initializeComponent(builderFromStringRepresentation("flowDependingOnConfigFromDomain").build());

    // Components in the app
    assertFlowsInitialized(appLocator, "flowDependingOnConfigFromDomain");
    assertLocationsInitialized(appLocator, "flowDependingOnConfigFromDomain", "flowDependingOnConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "anotherFlowDependingOnAnotherConfigFromDomain",
                                  "anotherFlowDependingOnAnotherConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "anotherFlowThatShouldNotBeInitialized");

    // Components in the domain
    assertLocationsInitialized(domainLocator, "configInDomain");
    assertLocationsNotInitialized(domainLocator, "anotherConfigThatShouldNotBeInitialized");
  }

  @Test
  public void whenInitializingAdditionalComponentsDependingOnComponentInParentArtifactThenBothAreInitialized() {
    // Initializes a flow with a listener depending on a config from the domain
    appLazyComponentInitializer
        .initializeComponent(builderFromStringRepresentation("anotherFlowDependingOnAnotherConfigFromDomain").build());

    assertLocationsInitialized(appLocator, "anotherFlowDependingOnAnotherConfigFromDomain",
                               "anotherFlowDependingOnAnotherConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "flowDependingOnConfigFromDomain");

    // Initializes another flow with a listener depending on another config from the domain
    getInfrastructureForApp(APP_NAME).getRegistry().lookupByName("flowDependingOnConfigFromDomain");

    // Components in the app
    assertFlowsInitialized(appLocator, "anotherFlowDependingOnAnotherConfigFromDomain", "flowDependingOnConfigFromDomain");
    assertLocationsInitialized(appLocator, "anotherFlowDependingOnAnotherConfigFromDomain",
                               "anotherFlowDependingOnAnotherConfigFromDomain/source");
    assertLocationsInitialized(appLocator, "flowDependingOnConfigFromDomain", "flowDependingOnConfigFromDomain/source");
    assertLocationsNotInitialized(appLocator, "anotherFlowThatShouldNotBeInitialized");

    // Components in the domain
    assertLocationsInitialized(domainLocator, "configInDomain");
    assertLocationsInitialized(domainLocator, "anotherConfigInDomain");
    assertLocationsNotInitialized(domainLocator, "anotherConfigThatShouldNotBeInitialized");
  }

  private LazyComponentInitializer getLazyComponentInitializerForApp(String appName) {
    return getLazyComponentInitializerFromArtifactInfrastructure(getInfrastructureForApp(appName));
  }

  private ConfigurationComponentLocator getLocatorForApp(String appName) {
    return getLocatorFromArtifactInfrastructure(getInfrastructureForApp(appName));
  }

  private ConfigurationComponentLocator getLocatorForDomain() {
    return getLocatorFromArtifactInfrastructure(getDomainInfrastructure());
  }

  private LazyComponentInitializer getLazyComponentInitializerFromArtifactInfrastructure(ArtifactInstanceInfrastructure infrastructure) {
    return infrastructure.getRegistry().lookupByType(LazyComponentInitializer.class).get();
  }

  private ConfigurationComponentLocator getLocatorFromArtifactInfrastructure(ArtifactInstanceInfrastructure infrastructure) {
    return infrastructure.getRegistry().lookupByType(ConfigurationComponentLocator.class).get();
  }

  private void assertFlowsInitialized(ConfigurationComponentLocator locator, String... expectedFlowNames) {
    List<String> flowNames = locator.find(buildFromStringRepresentation("flow")).stream()
        .map(c -> ((NamedObject) c).getName())
        .collect(toList());
    assertThat(flowNames, containsInAnyOrder(expectedFlowNames));
  }

  private void assertLocationsInitialized(ConfigurationComponentLocator locator, String... expectedLocations) {
    for (String location : expectedLocations) {
      assertThat(format("%s is not initialized", location),
                 locator.find(builderFromStringRepresentation(location).build()),
                 is(not(empty())));
    }
  }

  private void assertLocationsNotInitialized(ConfigurationComponentLocator locator, String... expectedLocations) {
    for (String location : expectedLocations) {
      assertThat(format("%s is initialized", location),
                 locator.find(builderFromStringRepresentation(location).build()),
                 is(empty()));
    }
  }

  private static ExtensionModel loadJavaSdkExtension(Class<?> extensionClass, ClassLoader classLoader,
                                                     Set<ExtensionModel> dependencies) {
    ExtensionModelLoadingRequest loadingRequest = builder(classLoader, getDefault(dependencies))
        .addParameter(TYPE_PROPERTY_NAME, extensionClass.getName())
        .addParameter(VERSION, "1.0.0-SNAPSHOT")
        .build();
    return new DefaultJavaExtensionModelLoader().loadExtensionModel(loadingRequest);
  }
}
