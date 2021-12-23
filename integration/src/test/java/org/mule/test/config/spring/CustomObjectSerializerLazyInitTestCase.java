package org.mule.test.config.spring;

import io.qameta.allure.Description;
import io.qameta.allure.Issue;
import org.junit.Test;
import org.mule.runtime.api.component.location.Location;
import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.runtime.config.api.LazyComponentInitializer;
import org.mule.test.AbstractIntegrationTestCase;

import javax.inject.Inject;
import javax.inject.Named;

import static org.junit.Assert.assertTrue;
import static org.mule.runtime.api.component.location.Location.builder;
import static org.mule.runtime.config.api.LazyComponentInitializer.LAZY_COMPONENT_INITIALIZER_SERVICE_KEY;

public class CustomObjectSerializerLazyInitTestCase extends AbstractIntegrationTestCase {

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
    Location location = builder().globalName("flow1").build();
    lazyComponentInitializer.initializeComponent(location);
    assertTrue(muleContext.getObjectSerializer() instanceof TestSerializationProtocol);

    /* initialize another component so beans get regenerated */
    location = builder().globalName("flow2").build();
    lazyComponentInitializer.initializeComponent(location);
    assertTrue(muleContext.getObjectSerializer() instanceof TestSerializationProtocol);
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
