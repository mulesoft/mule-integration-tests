/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.integration.transformer.response;

public class BindingInterfaceComponent {

  BindingInterface binding;

  public String invoke(String s) {
    s = binding.hello1(s);
    s = binding.hello2(s);
    // s = binding.hello3(s);
    // s = binding.hello4(s);
    return s;
  }

  public void setBinding(BindingInterface binding) {
    this.binding = binding;
  }

  public BindingInterface getBinding() {
    return binding;

  }

}
