/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Applies the given {@link Matcher} to the given {@link Header} of an {@link HttpResponse}.
 */
public class HttpResponseHeaderStringMatcher extends TypeSafeMatcher<HttpResponse> {

  private String headerName;
  private Matcher<String> matcher;

  public HttpResponseHeaderStringMatcher(String headerNameGiven, Matcher<String> matcherGiven) {
    headerName = headerNameGiven;
    matcher = matcherGiven;
  }

  @Override
  public boolean matchesSafely(HttpResponse response) {
    Header header = response.getFirstHeader(headerName);
    return header != null && matcher.matches(header.getValue());
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a response that has the header <" + headerName + "> that ").appendDescriptionOf(matcher);
  }

  @Override
  protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendValue(response.getFirstHeader(headerName));
  }

  public static Matcher<HttpResponse> header(String headerName, Matcher<String> matcher) {
    return new HttpResponseHeaderStringMatcher(headerName, matcher);
  }
}
