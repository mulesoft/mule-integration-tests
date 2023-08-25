/*
 * Copyright 2023 Salesforce, Inc. All rights reserved.
 */
package org.mule.test;

import static org.mule.runtime.container.api.ContainerClassLoaderProvider.createContainerClassLoader;
import static org.mule.runtime.module.artifact.api.descriptor.BundleScope.COMPILE;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toSet;

import static org.openjdk.jmh.annotations.Mode.AverageTime;
import static org.openjdk.jmh.annotations.Scope.Benchmark;

import org.mule.runtime.container.api.ModuleRepository;
import org.mule.runtime.container.api.MuleModule;
import org.mule.runtime.module.artifact.activation.internal.classloader.DefaultArtifactClassLoaderResolver;
import org.mule.runtime.module.artifact.activation.internal.nativelib.DefaultNativeLibraryFinderFactory;
import org.mule.runtime.module.artifact.api.classloader.MuleArtifactClassLoader;
import org.mule.runtime.module.artifact.api.classloader.MuleDeployableArtifactClassLoader;
import org.mule.runtime.module.artifact.api.descriptor.ApplicationDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.ArtifactPluginDescriptor;
import org.mule.runtime.module.artifact.api.descriptor.BundleDependency;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration;
import org.mule.runtime.module.artifact.api.descriptor.ClassLoaderConfiguration.ClassLoaderConfigurationBuilder;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

@Fork(1)
@OutputTimeUnit(MILLISECONDS)
@State(Benchmark)
public class PluginClassloaderCreationBenchmark extends AbstractArtifactActivationBenchmark {

  public static final String MULE_DOMAIN_FOLDER = "domains";
  private static final String PRIVILEGED_PACKAGE = "org.foo.privileged";
  private static final String PLUGIN_ARTIFACT_ID1 = GROUP_ID + ":" + PLUGIN_ID1;
  private final ArtifactPluginDescriptor plugin2DescriptorWithPrivilege = new ArtifactPluginDescriptor(PLUGIN_ID2);

  private final DefaultNativeLibraryFinderFactory nativeLibraryFinderFactory = new DefaultNativeLibraryFinderFactory();
  private final String customDomainName = "custom-domain";
  private final String applicationName = "app";
  private final String pluginPackage = "plugin-package";
  private final String plugin2ExportedPackage = "plugin2-package";
  private final String package1Name = "module&plugin-package";
  private final String package2Name = "org.mule.sdk.api.package";
  private MuleDeployableArtifactClassLoader applicationClassLoader;
  private ClassLoaderConfiguration pluginDependantClassLoaderConfiguration;
  private ClassLoaderConfiguration pluginExportingPackageClassLoaderConfiguration;
  private ClassLoaderConfiguration plugin2ExportingPackageClassLoaderConfiguration;
  private List<MuleModule> privilegeMuleModuleSingletonList;
  private ClassLoaderConfiguration pluginDependantWithLocalPackageClassLoaderConfiguration;
  private ClassLoaderConfiguration pluginWithLocalPackageClassLoaderConfiguration;

  private MuleModule muleModule;
  private List<MuleModule> muleModuleSingletonList;

  private DefaultArtifactClassLoaderResolver artifactClassLoaderResolverForPrivilegedContainerAccess;
  private ModuleRepository moduleRepositoryForPrivilegedContainerAccess;

  @Override
  @Setup
  public void setup() throws IOException {
    super.setup();
    customDomainDescriptor = getTestDomainDescriptor(customDomainName);
    customDomainDescriptor.setRootFolder(createDomainDir(MULE_DOMAIN_FOLDER, customDomainName));

    MuleModule privilegedModule =
        new MuleModule("TEST", emptySet(), emptySet(), singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1),
                       emptyList());
    privilegeMuleModuleSingletonList = singletonList(privilegedModule);

    ApplicationDescriptor applicationDescriptor = new ApplicationDescriptor(applicationName);
    applicationDescriptor.setArtifactLocation(new File(muleHomeFolder, applicationName));

    applicationClassLoader = getTestApplicationClassLoader(emptyList());
    BundleDependency pluginDependency = new BundleDependency.Builder().setScope(COMPILE).setDescriptor(PLUGIN2_BUNDLE_DESCRIPTOR)
        .setBundleUri(new File("test").toURI()).build();
    pluginDependantClassLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().dependingOn(singleton(pluginDependency)).build();
    pluginExportingPackageClassLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().exportingPackages(singleton(plugin2ExportedPackage))
            .build();

    ClassLoaderConfiguration plugin2ClassLoaderConfiguration = new ClassLoaderConfigurationBuilder()
        .exportingPrivilegedPackages(singleton(PRIVILEGED_PACKAGE), singleton(PLUGIN_ARTIFACT_ID1)).build();
    plugin2DescriptorWithPrivilege.setClassLoaderConfiguration(plugin2ClassLoaderConfiguration);

    plugin2ExportingPackageClassLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().exportingPackages(singleton(pluginPackage)).build();
    pluginDependantWithLocalPackageClassLoaderConfiguration =
        new ClassLoaderConfigurationBuilder().withLocalPackages(singleton(pluginPackage))
            .dependingOn(singleton(pluginDependency)).build();

    muleModule = new MuleModule("TEST", Stream.of(package1Name, package2Name).collect(toSet()), emptySet(), emptySet(),
                                emptySet(), emptyList());
    muleModuleSingletonList = singletonList(muleModule);
    ModuleRepository moduleRepositoryWithModules = new DummyModuleRepository(muleModuleSingletonList);
    artifactClassLoaderResolverWithModules =
        new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepositoryWithModules),
                                               moduleRepositoryWithModules, nativeLibraryFinderFactory);

    pluginWithLocalPackageClassLoaderConfiguration = new ClassLoaderConfigurationBuilder()
        .withLocalPackages(Stream.of(package1Name, package2Name).collect(toSet())).build();

    moduleRepositoryForPrivilegedContainerAccess = new DummyModuleRepository(privilegeMuleModuleSingletonList);
    artifactClassLoaderResolverForPrivilegedContainerAccess =
        new DefaultArtifactClassLoaderResolver(createContainerClassLoader(moduleRepositoryForPrivilegedContainerAccess),
                                               moduleRepositoryForPrivilegedContainerAccess, nativeLibraryFinderFactory);

  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createDependantPluginClassLoader() {
    plugin1Descriptor.setClassLoaderConfiguration(pluginDependantClassLoaderConfiguration);
    plugin2Descriptor.setClassLoaderConfiguration(pluginExportingPackageClassLoaderConfiguration);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   (apds, d) -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithPrivilegedContainerAccess() {
    return artifactClassLoaderResolverForPrivilegedContainerAccess
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor, (apds, d) -> empty());
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createsPluginClassLoaderWithPrivilegedPluginAccess() {
    plugin1Descriptor.setClassLoaderConfiguration(pluginDependantClassLoaderConfiguration);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   (apds, d) -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithExportedLocalPackage() {
    plugin2Descriptor.setClassLoaderConfiguration(plugin2ExportingPackageClassLoaderConfiguration);
    plugin1Descriptor.setClassLoaderConfiguration(pluginDependantWithLocalPackageClassLoaderConfiguration);
    return artifactClassLoaderResolver.createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor,
                                                                   (apds, d) -> of(plugin2Descriptor));
  }

  @Benchmark
  @BenchmarkMode(AverageTime)
  public MuleArtifactClassLoader createPluginClassLoaderWithIgnoredLocalPackages() {
    plugin1Descriptor.setClassLoaderConfiguration(pluginWithLocalPackageClassLoaderConfiguration);
    return artifactClassLoaderResolverWithModules
        .createMulePluginClassLoader(applicationClassLoader, plugin1Descriptor, (apds, d) -> empty());
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
    descriptor.setClassLoaderConfiguration(new ClassLoaderConfigurationBuilder().exportingPackages(exportedPackages).build());

    return artifactClassLoaderResolver.createApplicationClassLoader(descriptor, () -> domainClassLoader);
  }
}
