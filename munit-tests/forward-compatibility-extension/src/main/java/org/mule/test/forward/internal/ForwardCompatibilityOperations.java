/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
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
