/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.properties;

import static org.mule.runtime.config.spring.api.SpringXmlConfigurationBuilderFactory.createConfigurationBuilder;

import org.mule.runtime.core.api.config.ConfigurationException;
import org.mule.runtime.core.api.context.DefaultMuleContextFactory;
import org.mule.tck.junit4.AbstractMuleTestCase;

import org.junit.Test;

public class InvalidSetVariableTestCase extends AbstractMuleTestCase {

  private String muleConfigPath = "org/mule/properties/invalid-set-variable.xml";

  @Test(expected = ConfigurationException.class)
  public void emptyVariableNameValidatedBySchema() throws Exception {
    // TODO MULE-10061 - Review once the MuleContext lifecycle is clearly defined
    new DefaultMuleContextFactory().createMuleContext(createConfigurationBuilder(muleConfigPath));
  }
}
