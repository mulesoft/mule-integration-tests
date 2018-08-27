/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test.integration.classloading;

import org.mule.runtime.api.exception.MuleRuntimeException;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * This class will be executed as if belonging to an application. It allows testing which resources will be accessible to an app
 * classloader.
 */
public class ResourceAccessor {

  public static String access(String resource) {
    InputStream resourceStream = ResourceAccessor.class.getClassLoader().getResourceAsStream(resource);
    if (resourceStream != null) {
      Properties properties = new Properties();
      try {
        properties.load(resourceStream);
      } catch (IOException e) {
        throw new MuleRuntimeException(e);
      }
      return properties.getProperty("Bundle-Description");
    }
    return "";
  }

}
