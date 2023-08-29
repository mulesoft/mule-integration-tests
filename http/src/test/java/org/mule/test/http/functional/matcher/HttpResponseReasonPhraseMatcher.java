/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

public class HttpResponseReasonPhraseMatcher extends TypeSafeMatcher<HttpResponse> {

  private String reasonPhrase;

  public HttpResponseReasonPhraseMatcher(String reasonPhrase) {
    this.reasonPhrase = reasonPhrase;
  }

  @Override
  public boolean matchesSafely(HttpResponse response) {
    return reasonPhrase.equals(response.getStatusLine().getReasonPhrase());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a response with reason phrase ").appendValue(reasonPhrase);
  }

  @Override
  protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription) {
    mismatchDescription.appendText("got a response with reason phrase ").appendValue(response.getStatusLine().getReasonPhrase());
  }

  public static Matcher<HttpResponse> hasReasonPhrase(String reasonPhrease) {
    return new HttpResponseReasonPhraseMatcher(reasonPhrease);
  }
}
