/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring;

import org.mule.runtime.core.api.context.MuleContextAware;

public class DummyComponent {

  private MuleContextAware property;

  public MuleContextAware getProperty() {
    return property;
  }

  public void setProperty(MuleContextAware dummy) {
    this.property = dummy;
  }

}
