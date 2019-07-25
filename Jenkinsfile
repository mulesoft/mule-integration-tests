def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/mule/mule-4.2.1" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       // Comment the mule public settings as there are some transitive deps from the services that are only present
                       // in private repos like ci-releases and releases-ee.
                       //"mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Dmule.scheduler.alwaysShowSchedulerCreationLocation",
                       "additionalConfigFileProviderList" : [configFile(fileId: "mule-runtime-maven-settings-MuleSettings", variable: "org.mule.maven.client.api.SettingsSupplierFactory.userSettings")] ]

runtimeProjectsBuild(pipelineParams)
