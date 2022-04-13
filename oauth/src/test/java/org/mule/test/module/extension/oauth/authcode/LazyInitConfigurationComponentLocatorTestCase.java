/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.module.extension.oauth.authcode;

import static java.util.Optional.empty;
import static java.util.stream.Collectors.toList;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.CONFIGURATION_COMPONENT_LOCATOR;
import static org.mule.test.allure.AllureConstants.ConfigurationComponentLocatorFeature.ConfigurationComponentLocatorStory.SEARCH_CONFIGURATION;

import org.mule.runtime.api.component.location.ComponentLocation;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.module.extension.oauth.BaseOAuthExtensionTestCase;

import java.util.List;

import javax.inject.Inject;

import org.junit.Test;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Issues;
import io.qameta.allure.Story;

@Feature(CONFIGURATION_COMPONENT_LOCATOR)
@Story(SEARCH_CONFIGURATION)
public class LazyInitConfigurationComponentLocatorTestCase extends BaseOAuthExtensionTestCase {

  @Inject
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String[] getConfigFiles() {
    return new String[] {
        "auth-code-oauth-extension-config.xml",
        "oauth-extension-flows.xml"
    };
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
               containsInAnyOrder(
                                  "listenerConfig",
                                  "listenerConfig/connection",

                                  "oauth",
                                  "oauth/connection",
                                  "oauth/connection/1/0",
                                  "oauth/connection/1/2",
                                  "oauth/connection/3/0",
                                  "oauth/connection/1/1",
                                  "oauth/connection/2",
                                  "oauth/connection/0/0",
                                  "oauth/connection/0/1",

                                  "getConnection",
                                  "getConnection/processors/0",

                                  "beforeFlow",
                                  "beforeFlow/processors/0",

                                  "afterFlow",
                                  "afterFlow/processors/0",
                                  "afterFlow/processors/1",

                                  "refreshToken",
                                  "refreshToken/processors/0",

                                  "refreshTokenAsync",
                                  "refreshTokenAsync/processors/0",

                                  "unauthorize",
                                  "unauthorize/processors/0",

                                  "pagedOperationFailsAtFirstPage",
                                  "pagedOperationFailsAtFirstPage/processors/0",

                                  "pagedOperationFailsAtThirdPage",
                                  "pagedOperationFailsAtThirdPage/processors/0",
                                  "pagedOperationFailsAtThirdPage/processors/1",
                                  "pagedOperationFailsAtThirdPage/processors/2",
                                  "pagedOperationFailsAtThirdPage/processors/2/processors/0",

                                  "metadata",
                                  "metadata/processors/0",

                                  "anotherMetadata",
                                  "anotherMetadata/processors/0",

                                  "entitiesMetadata",
                                  "entitiesMetadata/processors/0",

                                  "values",
                                  "values/processors/0",

                                  "sampleData",
                                  "sampleData/processors/0"));

    assertThat(locator.find(builder().globalName("oauth").build()), is(empty()));
    assertThat(locator.find(builder().globalName("listenerConfig").build()), is(empty()));
    assertThat(locator.find(builder().globalName("beforeFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("afterFlow").build()), is(empty()));
    assertThat(locator.find(builder().globalName("unauthorize").build()), is(empty()));
  }

  @Test
  @Issues({@Issue("MULE-18513"), @Issue("MULE-18528")})
  public void oauthAuthCodeCallbackInitializesHttpListener() {
    lazyComponentInitializer.initializeComponent(builder().globalName("oauth").build());
    assertThat(locator.find(builder().globalName("oauth").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("listenerConfig").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("beforeFlow").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("afterFlow").build()), is(not(empty())));
    assertThat(locator.find(builder().globalName("unauthorize").build()), is(empty()));
  }

}
