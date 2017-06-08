/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.tck;

import org.mule.tck.testmodels.fruit.Orange;

import org.springframework.beans.factory.FactoryBean;

/**
 * A Spring FactoryBean implementation for unit testing.
 */
public class FloridaSunnyOrangeFactoryBean implements FactoryBean {

  Integer segments = new Integer(10);
  Double radius = new Double(4.34);

  @Override
  public Object getObject() throws Exception {
    return new Orange(segments, radius, "Florida Sunny");
  }

  @Override
  public Class getObjectType() {
    return Orange.class;
  }

  @Override
  public boolean isSingleton() {
    return false;
  }

  public Double getRadius() {
    return radius;
  }

  public void setRadius(Double radius) {
    this.radius = radius;
  }

  public Integer getSegments() {
    return segments;
  }

  public void setSegments(Integer segments) {
    this.segments = segments;
  }
}
