/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.test.components.tracing;

import java.util.ArrayList;
import java.util.List;

public class SpanTestHierarchy {
  private SpanNode root;
  private SpanNode currentNode;
  private SpanNode lastChild;
  private final String NO_PARENT_SPAN = "0000000000000000";

  public class SpanNode{
    private String spanName;
    private SpanNode parent;
    private List<SpanNode> children =  new ArrayList<>();
    
    public SpanNode(String spanName){
      this.spanName = spanName;
    }
    
    public void addChild(SpanNode child){
      children.add(child);
    }
  }
  
  public SpanTestHierarchy withRoot(String rootName){
    root = new SpanNode(rootName);
    root.parent = new SpanNode(NO_PARENT_SPAN);
    currentNode = root;
    return this;
  }

  public SpanTestHierarchy withChildren(){
    lastChild = currentNode;
    return this;
  }

  public SpanTestHierarchy child(String childName){
    SpanNode child = new SpanNode(childName);
    child.parent = lastChild;
    lastChild.addChild(child);
    currentNode = child;
    return this;
  }
  
  public SpanTestHierarchy endChildren(){
    currentNode = lastChild;
    return this;
  }
}

    /*
    SpanHierarchy spanHierarchy = new SpanHierarchy()
      .withRoot("flowSpan")
      .withChildren()
        .span("setPayloadSpan")
        .span("scatterSpan")
        .withChildren()
          .span("routeSpan").matching(span -> span.correlationId == 0)
          .span("routeSpan")
        .endChildren()
        .withChildren
      .endChildren();
     */