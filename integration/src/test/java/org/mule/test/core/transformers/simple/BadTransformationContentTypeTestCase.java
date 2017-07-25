/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.core.transformers.simple;

import org.mule.test.AbstractIntegrationTestCase;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class BadTransformationContentTypeTestCase extends AbstractIntegrationTestCase {

  @Rule
  public ExpectedException expected = ExpectedException.none();

  public BadTransformationContentTypeTestCase() {
    setStartContext(false);
  }

  @Override
  protected String getConfigFile() {
    return "bad-content-type-setting-transformer-configs.xml";
  }

  @Test
  public void testReturnType() throws Exception {
    muleContext.start();

    expected.expect(new TypeSafeMatcher<Exception>() {

      private Exception item;

      @Override
      protected boolean matchesSafely(Exception item) {
        this.item = item;
        return "org.springframework.beans.factory.BeanCreationException".equals(item.getClass().getName());
      }

      @Override
      public void describeTo(Description description) {
        description.appendText("was: ");
        item.printStackTrace();
      }
    });
    muleContext.getRegistry().lookupTransformer("testTransformer");
  }
}
