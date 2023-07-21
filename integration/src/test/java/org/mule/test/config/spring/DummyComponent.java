/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
