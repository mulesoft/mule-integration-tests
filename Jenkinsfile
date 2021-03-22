def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/mule/4.1.6-APRIL-2021" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Dmule.scheduler.alwaysShowSchedulerCreationLocation",
                       "additionalConfigFileProviderList" : [configFile(fileId: "mule-runtime-maven-settings-MuleSettings", variable: "org.mule.maven.client.api.SettingsSupplierFactory.userSettings")],
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
