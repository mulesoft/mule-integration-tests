/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.construct;

import static org.hamcrest.Matchers.is;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.CORE_COMPONENTS;
import static org.mule.test.allure.AllureConstants.ComponentsFeature.FlowReferenceStory.FLOW_REFERENCE;
import org.mule.functional.junit4.ApplicationContextBuilder;
import org.mule.tck.junit4.AbstractMuleTestCase;

import io.qameta.allure.Feature;
import io.qameta.allure.Story;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

@Feature(CORE_COMPONENTS)
@Story(FLOW_REFERENCE)
public class FlowRefBadConfigTestCase extends AbstractMuleTestCase {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Test
  public void flowRefToNonExistentFlow() throws Exception {
    expectedException.expectMessage(
                                    is("flow-ref at org/mule/test/construct/flow-ref-non-existent-flow.xml:6 is pointing to sub-flow-name which does not exist"));
    new ApplicationContextBuilder()
        .setApplicationResources(new String[] {"org/mule/test/construct/flow-ref-non-existent-flow.xml"}).build();
  }

}
