/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.runtime.test.integration.logging.plugin;

import static org.mule.runtime.api.deployment.meta.Product.MULE;
import static org.mule.runtime.module.artifact.api.descriptor.ArtifactDescriptorConstants.MULE_LOADER_ID;
import static org.mule.runtime.module.artifact.api.descriptor.BundleDescriptor.MULE_PLUGIN_CLASSIFIER;
import static org.mule.runtime.module.deployment.impl.internal.policy.PropertiesBundleDescriptorLoader.PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID;
import static org.mule.runtime.module.deployment.internal.util.Utils.createBundleDescriptorLoader;
import static org.mule.runtime.module.deployment.internal.util.Utils.getResourceFile;
import static org.mule.runtime.module.extension.internal.loader.java.DefaultJavaExtensionModelLoader.JAVA_LOADER_ID;

import org.mule.runtime.api.deployment.meta.MuleArtifactLoaderDescriptorBuilder;
import org.mule.runtime.api.deployment.meta.MulePluginModel;
import org.mule.runtime.module.deployment.impl.internal.builder.ArtifactPluginFileBuilder;
import org.mule.runtime.module.deployment.impl.internal.builder.JarFileBuilder;
import org.mule.tck.util.CompilerUtils;

import java.io.File;
import java.net.URISyntaxException;

public class TestPluginsCatalog {

  private static final String MIN_MULE_VERSION = "4.0.0";

  public static ArtifactPluginFileBuilder loggingExtensionPlugin;

  public static File loggingExtensionV1JarFile;

  static {
    try {
      initArtifactPluginFileBuilders();
    } catch (URISyntaxException e) {
      throw new RuntimeException(e);
    }
  }

  private static void initArtifactPluginFileBuilders() throws URISyntaxException {
    loggingExtensionPlugin = createLoggingExtensionV1PluginFileBuilder();
  }

  private static ArtifactPluginFileBuilder createLoggingExtensionV1PluginFileBuilder() throws URISyntaxException {

    loggingExtensionV1JarFile = new CompilerUtils.ExtensionCompiler()
        .compiling(getResourceFile("/org/foo/logging/LoggingExtension.java"),
                   getResourceFile("/org/foo/logging/LoggingOperation.java"))
        .compile("mule-module-logging-1.0.0.jar", "1.0.0");

    MulePluginModel.MulePluginModelBuilder mulePluginModelBuilder = new MulePluginModel.MulePluginModelBuilder()
        .setMinMuleVersion(MIN_MULE_VERSION).setName("loggingExtensionPlugin").setRequiredProduct(MULE)
        .withBundleDescriptorLoader(createBundleDescriptorLoader("loggingExtensionPlugin", MULE_PLUGIN_CLASSIFIER,
                                                                 PROPERTIES_BUNDLE_DESCRIPTOR_LOADER_ID, "1.0.0"));
    mulePluginModelBuilder.withClassLoaderModelDescriptorLoader(new MuleArtifactLoaderDescriptorBuilder().setId(MULE_LOADER_ID)
        .build());
    mulePluginModelBuilder.withExtensionModelDescriber().setId(JAVA_LOADER_ID)
        .addProperty("type", "org.foo.logging.LoggingExtension")
        .addProperty("version", "1.0.0");
    return new ArtifactPluginFileBuilder("loggingExtensionPlugin-1.0.0")
        .dependingOn(new JarFileBuilder("loggingExtensionV1", loggingExtensionV1JarFile))
        .describedBy((mulePluginModelBuilder.build()));
  }
}
