/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package extension.org.mule.soap.it;

import org.mule.runtime.extension.api.annotation.Extension;
import org.mule.runtime.extension.api.soap.annotation.Soap;
import org.mule.runtime.extension.api.soap.annotation.SoapMessageDispatcherProviders;
import org.mule.runtime.module.extension.soap.api.runtime.connection.transport.DefaultHttpMessageDispatcherProvider;

@Soap({TestServiceProvider.class, TestServiceProviderWithCustomHeaders.class})
@SoapMessageDispatcherProviders({TestHttpDispatcherProvider.class, TestExtensionsClientHttpDispatcherProvider.class})
@Extension(name = "simple")
public class SoapTestExtension {

}
