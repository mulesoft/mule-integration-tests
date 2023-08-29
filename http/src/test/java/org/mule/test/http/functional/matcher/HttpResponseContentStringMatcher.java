/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.mule.runtime.core.api.util.IOUtils;

import java.io.IOException;

import org.apache.http.HttpResponse;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;

/**
 * Applies the given {@link Matcher} to the body of an {@link HttpResponse} converted to a string.
 */
public class HttpResponseContentStringMatcher extends TypeSafeMatcher<HttpResponse> {

  private Matcher<String> matcher;
  private String responseContent = null;

  public HttpResponseContentStringMatcher(Matcher<String> matcherToUse) {
    matcher = matcherToUse;
  }

  @Override
  public boolean matchesSafely(HttpResponse response) {
    try {
      responseContent = IOUtils.toString(response.getEntity().getContent());
    } catch (IOException e) {
      responseContent = null;
    }
    return matcher.matches(responseContent);
  }

  @Override
  public void describeTo(Description description) {
    description.appendText("a response which body ").appendDescriptionOf(matcher);
  }

  @Override
  protected void describeMismatchSafely(HttpResponse response, Description mismatchDescription) {
    mismatchDescription.appendText("was ").appendValue(responseContent);
  }

  public static Matcher<HttpResponse> body(Matcher<String> matcher) {
    return new HttpResponseContentStringMatcher(matcher);
  }
}
