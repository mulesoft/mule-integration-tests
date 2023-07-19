/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.foo.logging;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.annotation.Operations;
import org.mule.runtime.extension.api.annotation.param.Parameter;

import javax.inject.Inject;

/**
 * Extension for testing purposes
 */
@Extension(name = "logging")
@Operations({LoggingOperation.class})
public class LoggingExtension {

    @Parameter
    private String message;

    public LoggingExtension() {}

    public String getMessage() {
        return message;
    }
}
