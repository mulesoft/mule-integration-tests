/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.mule.extension.http.api.HttpResponseAttributes;

import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseAttributesReasonPhraseMatcher extends TypeSafeMatcher<HttpResponseAttributes> {

  private final String reasonPhrase;

  public HttpResponseAttributesReasonPhraseMatcher(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  @Override
  protected boolean matchesSafely(HttpResponseAttributes item) {
    return reasonPhrase.equals(item.getReasonPhrase());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("response attributes with reason phrase ").appendValue(reasonPhrase);
  }

  @Override
  protected void describeMismatchSafely(HttpResponseAttributes attributes, Description mismatchDescription) {
    mismatchDescription.appendText("got response attributes with reason phrase ").appendValue(attributes.getReasonPhrase());
  }
}
