/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.test.integration.properties;

import static java.lang.System.lineSeparator;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.CONFIGURATION_PROPERTIES;
import static org.mule.test.allure.AllureConstants.ConfigurationProperties.ComponentConfigurationAttributesStory.CONFIGURATION_PROPERTIES_RESOLVER_STORY;

import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.functional.junit4.DomainContextBuilder;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.runtime.core.api.MuleContext;
import org.mule.runtime.core.internal.registry.DefaultRegistry;
import org.mule.runtime.deployment.model.api.artifact.ArtifactContext;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.After;
import org.junit.Test;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_PROPERTIES)
@Story(CONFIGURATION_PROPERTIES_RESOLVER_STORY)
public class DomainPropertiesPlaceHolderPropagationTestCase extends AbstractMuleTestCase {

  private MuleContext domainContext;
  private MuleContext applicationContext;

  @Test
  public void propertiesPropagatesToAppUsingContext() throws Exception {
    configureContexts("properties/domain/shared-context-properties.xml", "properties/domain/app-with-no-properties.xml");
    propertiesPropagatesScenario();
  }

  @Test
  public void appPropertiesPrecedeDomainPropertiesUsingContext() throws Exception {
    configureContexts("properties/domain/shared-context-properties.xml", "properties/domain/app-with-context-properties.xml");
    appPropertiesPrecedeDomainPropertiesScenario();
  }

  private void appPropertiesPrecedeDomainPropertiesScenario() {
    String domainPropertyObject = getDomainProperty("domainPropertyObject");
    assertThat(domainPropertyObject, is("9999"));
    String appPropertyObject = getApplicationProperty("appPropertyObject");
    assertThat(appPropertyObject, is("10000"));
    String app2PropertyObject = getApplicationProperty("app2PropertyObject");
    assertThat(app2PropertyObject, is("service"));
  }

  private void propertiesPropagatesScenario() {
    String domainPropertyObject = getDomainProperty("domainPropertyObject");
    assertThat(domainPropertyObject, is("9999"));
    String appPropertyObject = getApplicationProperty("appPropertyObject");
    assertThat(appPropertyObject, is("9999"));
    String inlinePropertyObject = getApplicationProperty("inlinePropertyObject");
    assertThat(inlinePropertyObject, is("file contents\n"));
  }

  private String getApplicationProperty(String property) {
    return new DefaultRegistry(applicationContext).<ConfigurationProperties>lookupByType(ConfigurationProperties.class).get()
        .resolveStringProperty(property).get();
  }

  private String getDomainProperty(String property) {
    return new DefaultRegistry(domainContext).<ConfigurationProperties>lookupByType(ConfigurationProperties.class).get()
        .resolveStringProperty(property).get();
  }

  private void configureContexts(String domainConfig, String appConfig) throws Exception {
    final ArtifactContext domainArtifactContext = new DomainContextBuilder()
        .setContextId(DomainPropertiesPlaceHolderPropagationTestCase.class.getSimpleName())
        .setDomainConfig(domainConfig)
        .build();
    domainContext = domainArtifactContext
        .getMuleContext();
    applicationContext = new ApplicationContextBuilder()
        .setContextId(DomainPropertiesPlaceHolderPropagationTestCase.class.getSimpleName())
        .setApplicationResources(appConfig)
        .setDomainArtifactContext(domainArtifactContext)
        .build();
  }

  @After
  public void after() {
    if (applicationContext != null) {
      applicationContext.dispose();
    }
    if (domainContext != null) {
      domainContext.dispose();
    }
  }
}
