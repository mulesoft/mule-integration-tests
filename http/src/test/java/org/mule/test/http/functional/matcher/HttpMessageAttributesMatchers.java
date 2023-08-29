/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test.http.functional.matcher;

import org.mule.extension.http.api.HttpResponseAttributes;

import org.hamcrest.Matcher;

public class HttpMessageAttributesMatchers {

  public static Matcher<HttpResponseAttributes> hasStatusCode(int statusCode) {
    return new HttpResponseAttributesStatusCodeMatcher(statusCode);
  }

  public static Matcher<HttpResponseAttributes> hasReasonPhrase(String reasonPhrase) {
    return new HttpResponseAttributesReasonPhraseMatcher(reasonPhrase);
  }
}
