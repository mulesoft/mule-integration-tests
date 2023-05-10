/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.foo.logging;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;
import static org.slf4j.LoggerFactory.getLogger;

import org.mule.runtime.extension.api.annotation.param.Config;
import org.mule.runtime.extension.api.annotation.param.MediaType;

import org.foo.logging.LoggingExtension;
import org.mule.runtime.extension.api.runtime.process.CompletionCallback;
import org.slf4j.Logger;

public class LoggingOperation {

    private static final Logger LOGGER = getLogger(LoggingOperation.class);
    public static final String SECOND_MESSAGE = "Second Message";
    public static final String NON_BLOCKING_MESSAGE = "Non Blocking Message";

    public LoggingOperation() {}

    @MediaType(value = TEXT_PLAIN, strict = false)
    public String log(@Config LoggingExtension config) {
        LOGGER.info(config.getMessage());
        return config.getMessage();
    }

    @MediaType(value = TEXT_PLAIN, strict = false)
    public String logWithMessage(@Config LoggingExtension config) {
        LOGGER.info(SECOND_MESSAGE);
        return SECOND_MESSAGE;
    }

    @MediaType(value = TEXT_PLAIN, strict = false)
    public void nonBlockingOperationLog(CompletionCallback<Object, Object> completionCallback) {
        LOGGER.info(NON_BLOCKING_MESSAGE);
        completionCallback.success(null);
    }
}