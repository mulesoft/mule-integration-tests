/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package extension.org.mule.soap.it;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.soap.HttpMessageDispatcherProvider;
import org.mule.runtime.extension.api.soap.annotation.Soap;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;

@Soap({TestServiceProvider.class, TestServiceProviderWithCustomHeaders.class})
@SoapMessageDispatcherProviders(HttpMessageDispatcherProvider.class)
@Extension(name = "simple")
public class SoapTestExtension {

}
