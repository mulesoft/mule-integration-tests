/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.config.spring.parsers.beans;

import java.util.List;
import java.util.Map;

public class AbstractBean {

  // test explicit list
  private List list;
  // test explicit map
  private Map map;
  // needed for spring
  private String name;
  // test direct attribute
  private String string;
  // test aliased attribute (alias as bar)
  private int foo;
  // test ignore
  private boolean ignored = true;

  public List getList() {
    return list;
  }

  public void setList(List list) {
    this.list = list;
  }

  public Map getMap() {
    return map;
  }

  public void setMap(Map map) {
    this.map = map;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getString() {
    return string;
  }

  public void setString(String string) {
    this.string = string;
  }

  public int getFoo() {
    return foo;
  }

  public void setFoo(int foo) {
    this.foo = foo;
  }

  public boolean isIgnored() {
    return ignored;
  }

  public void setIgnored(boolean ignored) {
    this.ignored = ignored;
  }

}
