/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.config.spring.parsers.beans;

public class ThirdPartyContainer {

  private Object thing;

  public Object getThing() {
    return thing;
  }

  public void setThing(Object thing) {
    this.thing = thing;
  }

}
