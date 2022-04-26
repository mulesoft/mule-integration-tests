/*
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */
package org.mule.test;

import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;
import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderModel;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class PluginClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

  public static final String MULE_DOMAIN_FOLDER = "domains";
  private static final String PRIVILEGED_PACKAGE = "org.foo.privileged";
  private static final String PLUGIN_ARTIFACT_ID1 = GROUP_ID + ":" + PLUGIN_ID1;
  private final ArtifactPluginDescriptor plugin2DescriptorWithPrivilege = new ArtifactPluginDescriptor(PLUGIN_ID2);

  private final ModuleRepository moduleRepository = mock(ModuleRepository.class);
  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();
  private final String customDomainName = "custom-domain";
  private final String applicationName = "app";
  private final String pluginPackage = "plugin-package";
  private final String plugin2ExportedPackage = "plugin2-package";
  private final String package1Name = "module&plugin-package";
  private final String package2Name = "org.mule.sdk.api.package";
  private MuleDeployableArtifactClassLoader applicationClassLoader;
  private ClassLoaderModel pluginDependantClassLoaderModel;
  private ClassLoaderModel pluginExportingPackageClassLoaderModel;
  private ClassLoaderModel plugin2ExportingPackageClassLoaderModel;
  private List<MuleModule> privilegeMuleModuleSingletonList;
  private ClassLoaderModel pluginDependantWithLocalPackageClassLoaderModel;
  private ClassLoaderModel pluginWithLocalPackageClassLoaderModel;

  private DefaultArtifactClassLoaderResolver artifactClassLoaderResolverForPrivilegedContainerAccess;
  private final ModuleRepository moduleRepositoryForPrivilegedContainerAccess = mock(ModuleRepository.class);

  @Setup
  public void setup() throws IOException {
    super.setup();
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));

    MuleModule privilegedModule = mock(MuleModule.class);
    when(privilegedModule.getPrivilegedArtifacts()).thenReturn(singleton(PLUGIN_ARTIFACT_ID1));
    when(privilegedModule.getPrivilegedExportedPackages()).thenReturn(singleton(PRIVILEGED_PACKAGE));
    privilegeMuleModuleSingletonList = singletonList(privilegedModule);

    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(applicationName);
    applicationDescriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));

    applicationClassLoader = getTestApplicationClassLoader(emptyList());
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI()).build();
    pluginDependantClassLoaderModel =
        new ClassLoaderModel.ClassLoaderModelBuilder().dependingOn(singleton(pluginDependency)).build();
    pluginExportingPackageClassLoaderModel =
        new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(plugin2ExportedPackage))
            .build();

    ClassLoaderModel plugin2ClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2DescriptorWithPrivilege.setClassLoaderModel(plugin2ClassLoaderModel);

    plugin2ExportingPackageClassLoaderModel =
        new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(singleton(pluginPackage)).build();
    pluginDependantWithLocalPackageClassLoaderModel =
        new ClassLoaderModel.ClassLoaderModelBuilder().withLocalPackages(singleton(pluginPackage))
            .dependingOn(singleton(pluginDependency)).build();

    when(muleModule.getExportedPackages()).thenReturn(Stream.of(package1Name, package2Name).collect(toSet()));
    pluginWithLocalPackageClassLoaderModel = new ClassLoaderModel.ClassLoaderModelBuilder()
        .withLocalPackages(Stream.of(package1Name, package2Name).collect(toSet())).build();

    artifactClassLoaderResolverForPrivilegedContainerAccess =
        spy(new DefaultArtifactClassLoaderResolver(moduleRepositoryForPrivilegedContainerAccess, nativeLibraryFinderFactory));
    when(moduleRepositoryForPrivilegedContainerAccess.getModules()).thenReturn(privilegeMuleModuleSingletonList);
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createDependantPluginClassLoader() {
    plugin1Descriptor.setClassLoaderModel(pluginDependantClassLoaderModel);
    plugin2Descriptor.setClassLoaderModel(pluginExportingPackageClassLoaderModel);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   d -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithPrivilegedContainerAccess() {
    return artifactClassLoaderResolverForPrivilegedContainerAccess
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor, d -> empty());
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createsPluginClassLoaderWithPrivilegedPluginAccess() {
    plugin1Descriptor.setClassLoaderModel(pluginDependantClassLoaderModel);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   d -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithExportedLocalPackage() {
    plugin2Descriptor.setClassLoaderModel(plugin2ExportingPackageClassLoaderModel);
    plugin1Descriptor.setClassLoaderModel(pluginDependantWithLocalPackageClassLoaderModel);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   d -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithIgnoredLocalPackages() {
    plugin1Descriptor.setClassLoaderModel(pluginWithLocalPackageClassLoaderModel);
    return artifactClassLoaderResolverWithModules
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor, d -> empty());
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins) {
    return getTestApplicationClassLoader(plugins, emptySet());
  }

  private MuleDeployableArtifactClassLoader getTestApplicationClassLoader(List<ArtifactPluginDescriptor> plugins,
                                                                          Set<String> exportedPackages) {
    final MuleDeployableArtifactClassLoader domainClassLoader = getTestDomainClassLoader(emptyList());

    ApplicationDescriptor descriptor = new ApplicationDescriptor(applicationName);
    descriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));
    descriptor.setPlugins(new HashSet<>(plugins));
    descriptor.setClassLoaderModel(new ClassLoaderModel.ClassLoaderModelBuilder().exportingPackages(exportedPackages).build());

    return artifactClassLoaderResolver.createApplicationClassLoader(descriptor, () -> domainClassLoader);
  }
}
