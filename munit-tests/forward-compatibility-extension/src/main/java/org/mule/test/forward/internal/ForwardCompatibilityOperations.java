/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.forward.internal;

import static org.mule.runtime.extension.api.annotation.param.MediaType.TEXT_PLAIN;

import org.mule.runtime.extension.api.annotation.param.MediaType;
import org.mule.sdk.compatibility.api.utils.ForwardCompatibilityHelper;

import java.util.Optional;

import javax.inject.Inject;

public class ForwardCompatibilityOperations {

    @Inject
    private Optional<ForwardCompatibilityHelper> forwardCompatibilityHelper;

    @MediaType(value = TEXT_PLAIN)
    public String getHelperClassName(){
        return forwardCompatibilityHelper.map(fcHelper -> fcHelper.getClass().getName()).orElse("Helper not present");
    }
}
