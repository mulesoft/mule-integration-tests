/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import io.qameta.allure.Description;
import io.qameta.allure.Feature;
import io.qameta.allure.Issue;
import io.qameta.allure.Story;
import org.junit.Test;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;
import static org.mule.test.allure.AllureConstants.LazyInitializationFeature.LAZY_INITIALIZATION;
import static org.mule.test.allure.AllureConstants.ObjectSerializer.CUSTOM_OBJECT_SERIALIZER;

@Feature(LAZY_INITIALIZATION)
@Story(CUSTOM_OBJECT_SERIALIZER)
public class CustomObjectSerializerLazyInitTestCase extends AbstractIntegrationTestCase {

  private static final String FIRST_LOCATION = "flow1";
  private static final String SECOND_LOCATION = "flow2";

  @Inject
  @Named(value = LAZY_COMPONENT_INITIALIZER_SERVICE_KEY)
  private LazyComponentInitializer lazyComponentInitializer;

  @Override
  protected String getConfigFile() {
    return "custom-object-serializer-lazy-init.xml";
  }

  @Override
  public boolean enableLazyInit() {
    return true;
  }

  @Test
  @Issue("MULE-20012")
  @Description("Tests that the default object serializer gets correctly overridden whenever lazy initialization is run " +
      "more than once")
  public void lazyInitPerformedTwice() {
    Location location = builder().globalName(FIRST_LOCATION).build();
    lazyComponentInitializer.initializeComponent(location);
    assertThat(muleContext.getObjectSerializer(), instanceOf(TestSerializationProtocol.class));

    /* initialize another component so beans get regenerated */
    location = builder().globalName(SECOND_LOCATION).build();
    lazyComponentInitializer.initializeComponent(location);
    assertThat(muleContext.getObjectSerializer(), instanceOf(TestSerializationProtocol.class));
  }

  public static class TestSerializationProtocol implements ObjectSerializer {

    @Override
    public SerializationProtocol getInternalProtocol() {
      return null;
    }

    @Override
    public SerializationProtocol getExternalProtocol() {
      return null;
    }
  }
}
