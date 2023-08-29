/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseStatusCodeMatcher extends TypeSafeMatcher<HttpResponse> {

  private int statusCode;

  public HttpResponseStatusCodeMatcher(int statusCode) {
    this.statusCode = statusCode;
  }

  @Override
  public boolean matchesSafely(HttpResponse response) {
    return response.getStatusLine().getStatusCode() == statusCode;
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a response with status code ").appendValue(statusCode);
  }

  @Override
  protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription) {
    mismatchDescription.appendText("got a response with status code ").appendValue(response.getStatusLine().getStatusCode());
  }

  public static Matcher<HttpResponse> hasStatusCode(int statusCode) {
    return new HttpResponseStatusCodeMatcher(statusCode);
  }
}
