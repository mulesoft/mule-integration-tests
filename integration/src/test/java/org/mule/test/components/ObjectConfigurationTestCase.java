/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.components;

import static org.hamcrest.Matchers.containsString;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class ObjectConfigurationTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void objectMustContainRefOrClassAttribute() throws Exception {
    expectedException.expectMessage(containsString("Object must contain 'ref' or 'class' parameter."));
    new ApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/components/object-missing-ref-and-class-attributes-config.xml"})
        .build();
  }

  @Test
  public void objectCannotContainBothRefAndClassAttribute() throws Exception {
    expectedException.expectMessage(containsString("Object cannot contain both 'ref' and 'class' parameters."));
    new ApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/components/object-ref-and-class-attributes-config.xml"})
        .build();
  }

}
