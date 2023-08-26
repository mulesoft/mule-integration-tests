/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.CoreMatchers.is;

import com.google.common.collect.ImmutableMap;
import org.junit.Rule;
import org.junit.Test;
import org.mule.runtime.api.component.ConfigurationProperties;
import org.mule.tck.junit4.rule.SystemProperty;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import java.util.Map;

public class PropertiesHierarchyTestCase extends AbstractIntegrationTestCase {

  /**
   * Hierarchy should be: 1. Deployment Properties 2. System Properties 3. Environment Properties 4. Application Properties
   * (Configuration Properties, Secure Conf Properties, or any other Custom Conf) 5. Global Properties
   *
   * Also, lower-hierarchy props can depend on higher-hierarchy props
   */

  private static final String DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY = "Deployment wins";
  private static final String SYSTEM_PROPERTIES_HIGHER_HIERARCHY = "System wins";
  private static final String APPLICATION_PROPERTIES_HIGHER_HIERARCHY = "App wins";
  private static final String GLOBAL_PLUS_APP = "Global - App";
  private static final String GLOBAL_PLUS_SYSTEM = "Global - System";
  private static final String GLOBAL_PLUS_APP_PLUS_SYSTEM = "Global - App - System";
  private static final String APP_PLUS_GLOBAL = "App - Global";
  private static final String APP_PLUS_SYSTEM = "App - System";
  private static final String APP_PLUS_SYSTEM_VS_GLOBAL = "App - System wins";
  private static final String SYSTEM_PLUS_APP = "System - App";
  private static final String SYSTEM_PLUS_GLOBAL = "System - Global";
  private static final String DEPLOYMENT_VS_SYSTEM = "deploymentVsSystem";
  private static final String SYSTEM_VS_APP = "systemVsApp";
  private static final String SYSTEM_VS_GLOBAL = "systemVsGlobal";
  private static final String APP_VS_GLOBAL = "appVsGlobal";
  private static final String DEPLOYMENT_VS_GLOBAL = "deploymentVsGlobal";
  private static final String DEPLOYMENT_VS_APP = "deploymentVsApp";
  private static final String GLOBAL_DEP_APP = "globalDepApp";
  private static final String GLOBAL_DEP_SYSTEM = "globalDepSystem";
  private static final String APP_DEP_GLOBAL = "appDepGlobal";
  private static final String APP_DEP_SYSTEM = "appDepSystem";
  private static final String SYSTEM_DEP_APP = "systemDepApp";
  private static final String SYSTEM_DEP_GLOBAL = "systemDepGlobal";
  private static final String GLOBAL_DEP_APP_DEP_SYSTEM = "globalDepAppDepSystem";
  private static final String APP_WITH_OVERRIDED_DEPENDENCY = "appDepOverriden";

  @Inject
  private ConfigurationProperties configurationProperties;

  @Rule
  public SystemProperty deploymentVsSystem = new SystemProperty(DEPLOYMENT_VS_SYSTEM, SYSTEM_PROPERTIES_HIGHER_HIERARCHY);

  @Rule
  public SystemProperty systemVsApp = new SystemProperty(SYSTEM_VS_APP, SYSTEM_PROPERTIES_HIGHER_HIERARCHY);

  @Rule
  public SystemProperty systemVsGlobal = new SystemProperty(SYSTEM_VS_GLOBAL, SYSTEM_PROPERTIES_HIGHER_HIERARCHY);

  @Rule
  public SystemProperty systemValue = new SystemProperty("systemValue", "System");

  @Rule
  public SystemProperty systemDepApp = new SystemProperty(SYSTEM_DEP_APP, systemValue.getValue() + " - ${appValue}");

  @Rule
  public SystemProperty systemDepGlobal = new SystemProperty(SYSTEM_DEP_GLOBAL, systemValue.getValue() + " - ${globalValue}");


  @Override
  protected String getConfigFile() {
    return "org/mule/test/components/properties-hierarchy-config.xml";
  }


  @Override
  protected Map<String, String> artifactProperties() {
    return ImmutableMap.<String, String>builder()
        .put(DEPLOYMENT_VS_SYSTEM, DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY)
        .put(DEPLOYMENT_VS_APP, DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY)
        .put(DEPLOYMENT_VS_GLOBAL, DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY)
        .build();
  }

  @Test
  public void deploymentPropertiesHavePrecedenceOverSystemProperties() {
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_SYSTEM).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_SYSTEM).get(),
               is(DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void deploymentPropertiesHavePrecedenceOverApplicationProperties() {
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_APP).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_APP).get(),
               is(DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void deploymentPropertiesHavePrecedenceOverGlobalProperties() {
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_GLOBAL).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(DEPLOYMENT_VS_GLOBAL).get(),
               is(DEPLOYMENT_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void systemPropertiesHavePrecedenceOverApplicationProperties() {
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_VS_APP).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_VS_APP).get(), is(SYSTEM_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void systemPropertiesHavePrecedenceOverGlobalProperties() {
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_VS_GLOBAL).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_VS_GLOBAL).get(), is(SYSTEM_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void applicationPropertiesHavePrecedenceOverGlobalProperties() {
    assertThat(configurationProperties.resolveStringProperty(APP_VS_GLOBAL).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(APP_VS_GLOBAL).get(), is(APPLICATION_PROPERTIES_HIGHER_HIERARCHY));
  }

  @Test
  public void globalPropertiesCanDependOnApplicationProperties() {
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_APP).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_APP).get(), is(GLOBAL_PLUS_APP));
  }

  @Test
  public void applicationPropertiesCanDependOnGlobalProperties() {
    assertThat(configurationProperties.resolveStringProperty(APP_DEP_GLOBAL).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(APP_DEP_GLOBAL).get(), is(APP_PLUS_GLOBAL));
  }

  @Test
  public void globalPropertiesCanDependOnSystemProperties() {
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_SYSTEM).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_SYSTEM).get(), is(GLOBAL_PLUS_SYSTEM));
  }

  @Test
  public void systemPropertiesCanDependOnGlobalProperties() {
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_DEP_GLOBAL).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_DEP_GLOBAL).get(), is(SYSTEM_PLUS_GLOBAL));
  }

  @Test
  public void applicationPropertiesCanDependOnSystemProperties() {
    assertThat(configurationProperties.resolveStringProperty(APP_DEP_SYSTEM).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(APP_DEP_SYSTEM).get(), is(APP_PLUS_SYSTEM));
  }

  @Test
  public void systemPropertiesCanDependOnApplicationProperties() {
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_DEP_APP).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(SYSTEM_DEP_APP).get(), is(SYSTEM_PLUS_APP));
  }

  @Test
  public void globalPropertyDependsOnAppPropertyWhichDependsOnSystemProperty() {
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_APP_DEP_SYSTEM).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(GLOBAL_DEP_APP_DEP_SYSTEM).get(), is(GLOBAL_PLUS_APP_PLUS_SYSTEM));
  }

  @Test
  public void applicationPropertyCanDependOnPropertyWithOverridenValue() {
    assertThat(configurationProperties.resolveStringProperty(APP_WITH_OVERRIDED_DEPENDENCY).isPresent(), is(true));
    assertThat(configurationProperties.resolveStringProperty(APP_WITH_OVERRIDED_DEPENDENCY).get(), is(APP_PLUS_SYSTEM_VS_GLOBAL));
  }

}
