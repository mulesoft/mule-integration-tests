/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.api.serialization.ObjectSerializer.DEFAULT_OBJECT_SERIALIZER_NAME;

import org.mule.runtime.api.serialization.ObjectSerializer;
import org.mule.runtime.api.serialization.SerializationProtocol;
import org.mule.test.AbstractIntegrationTestCase;
import org.mule.test.runner.RunnerDelegateTo;

import org.junit.Test;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import javax.inject.Inject;
import javax.inject.Named;

@RunnerDelegateTo(Parameterized.class)
public class InjectDefaultObjectSerializerTestCase extends AbstractIntegrationTestCase {

  @Inject
  @Named(DEFAULT_OBJECT_SERIALIZER_NAME)
  private ObjectSerializer defaultSerializer;

  @Parameterized.Parameters(name = "{0}")
  public static Collection<Object[]> data() {
    return Arrays.asList(new Object[][] {{"Default Serializer", new String[] {}},
        {"Custom Serializer", new String[] {"custom-object-serializer-config.xml"}}});
  }

  private final String name;
  private final String[] configFiles;

  public InjectDefaultObjectSerializerTestCase(String name, String[] configFiles) {
    this.name = name;
    this.configFiles = configFiles;
  }

  @Override
  protected String[] getConfigFiles() {
    return configFiles;
  }

  @Test
  public void injectObjectSerializer() throws Exception {
    TestObjectSerializerInjectionTarget injectionTarget =
        muleContext.getInjector().inject(new TestObjectSerializerInjectionTarget());
    assertThat(muleContext.getObjectSerializer(), is(sameInstance(injectionTarget.getObjectSerializer())));
    assertThat(injectionTarget.getObjectSerializer(), is(sameInstance(defaultSerializer)));
  }

  public static class TestObjectSerializerInjectionTarget {

    @Inject
    @Named(DEFAULT_OBJECT_SERIALIZER_NAME)
    private ObjectSerializer objectSerializer;

    public ObjectSerializer getObjectSerializer() {
      return objectSerializer;
    }
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
