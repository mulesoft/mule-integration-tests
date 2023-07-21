/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.components;

import static org.mule.test.allure.AllureConstants.MuleDsl.DslValidationStory.DSL_VALIDATION_STORY;

import org.mule.functional.junit4.AbstractConfigurationFailuresTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import io.qameta.allure.Story;

@Story(DSL_VALIDATION_STORY)
public class ObjectConfigurationTestCase extends AbstractConfigurationFailuresTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void objectMustContainRefOrClassAttribute() throws Exception {
    expectedException.expectMessage("[org/mule/test/components/object-missing-ref-and-class-attributes-config.xml:5]:"
        + " Element <General> requires that one of its optional parameters must be set, but all of them are missing. One of the following must be set: [ref, class].");
    loadConfiguration("org/mule/test/components/object-missing-ref-and-class-attributes-config.xml");
  }

  @Test
  public void objectCannotContainBothRefAndClassAttribute() throws Exception {
    expectedException.expectMessage("[org/mule/test/components/object-ref-and-class-attributes-config.xml:6]: "
        + "Element <object>, the following parameters cannot be set at the same time: [ref, class]");
    loadConfiguration("org/mule/test/components/object-ref-and-class-attributes-config.xml");
  }

}
