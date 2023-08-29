/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.mule.extension.http.api.HttpResponseAttributes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseAttributesStatusCodeMatcher extends TypeSafeMatcher<HttpResponseAttributes> {

  private int statusCode;

  public HttpResponseAttributesStatusCodeMatcher(int statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  protected boolean matchesSafely(HttpResponseAttributes item) {
    return item.getStatusCode() == statusCode;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("response attributes with status code ").appendValue(statusCode);
  }

  @Override
  protected void describeMismatchSafely(HttpResponseAttributes attributes, Description mismatchDescription) {
    mismatchDescription.appendText("got response attributes with status code ").appendValue(attributes.getStatusCode());
  }
}
