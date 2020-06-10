/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.issues;

import io.qameta.allure.Issue;
import org.apache.http.client.fluent.Request;
import org.apache.http.client.fluent.Response;
import org.junit.Rule;
import org.junit.Test;
import org.mule.tck.junit4.rule.DynamicPort;
import org.mule.test.AbstractIntegrationTestCase;

import static org.apache.commons.lang3.StringUtils.repeat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mule.runtime.http.api.HttpConstants.HttpStatus.REQUEST_URI_TOO_LONG;

@Issue("MULE-18455")
public class HttpListenerUrlTooLongIssue18455TestCase extends AbstractIntegrationTestCase {

    @Rule
    public DynamicPort listenPort = new DynamicPort("port");

    @Test
    public void failsWithAppropriateError() throws Exception {
        final Response response = Request.Get(getListenerUrl(repeat("path", 3000)))
                .execute();

        assertThat(response.returnResponse().getStatusLine().getStatusCode(), is(REQUEST_URI_TOO_LONG.getStatusCode()));
    }

    @Override
    protected String getConfigFile() {
        return "org/mule/issues/http-listener-url-too-long-config.xml";
    }

    private String getListenerUrl(String path) {
        return String.format("http://localhost:%s/%s", listenPort.getNumber(), path);
    }
}
