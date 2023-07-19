/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.routing;

import java.io.Serializable;

/**
 * Simple Custom Serializable object to check that Custom Objects Can Actually be Chunked
 */
class SimpleSerializableObject implements Serializable {

  private static final long serialVersionUID = 4705305160224612898L;
  public String s;
  public boolean b;
  public int i;

  public SimpleSerializableObject(String s, boolean b, int i) {
    this.s = s;
    this.b = b;
    this.i = i;
  }
}
