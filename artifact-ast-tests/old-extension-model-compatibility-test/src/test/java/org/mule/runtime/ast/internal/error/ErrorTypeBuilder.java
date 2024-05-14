/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.runtime.ast.internal.error;

import org.mule.runtime.api.message.ErrorType;

/**
 * Adapter class to allow testing the AST mismatch from W-14722981 with the API changes from W-15402245.
 */
public class ErrorTypeBuilder implements org.mule.runtime.ast.api.error.ErrorTypeBuilder {

  public static final String CORE_NAMESPACE_NAME = DefaultErrorTypeBuilder.CORE_NAMESPACE_NAME;
  /**
   * Wild card that matches with any error and is on top of the error hierarchy for those that allow handling
   */
  public static final String ANY_IDENTIFIER = DefaultErrorTypeBuilder.ANY_IDENTIFIER;
  public static final String CRITICAL_IDENTIFIER = DefaultErrorTypeBuilder.CRITICAL_IDENTIFIER;
  private final org.mule.runtime.ast.api.error.ErrorTypeBuilder delegate;

  public static ErrorTypeBuilder builder() {
    return new ErrorTypeBuilder();
  }

  private ErrorTypeBuilder() {
    delegate = org.mule.runtime.ast.api.error.ErrorTypeBuilder.builder();
  }

  @Override
  public ErrorTypeBuilder identifier(String identifier) {
    delegate.identifier(identifier);
    return this;
  }

  @Override
  public ErrorTypeBuilder namespace(String namespace) {
    delegate.namespace(namespace);
    return this;
  }

  @Override
  public ErrorTypeBuilder parentErrorType(ErrorType parentErrorType) {
    delegate.parentErrorType(parentErrorType);
    return this;
  }

  /**
   * Creates a new instance of the configured error type.
   *
   * @return the error type with the provided configuration.
   */
  @Override
  public ErrorType build() {
    return delegate.build();
  }

}
