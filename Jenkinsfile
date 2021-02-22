def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/mule/4.2.0-FEBRUARY" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       // Comment public setting to get org.mule.runtime:api-annotations:jar:1.1.0-20200709 from private
                       // repo until it is released in a public repo
                       // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Dmule.scheduler.alwaysShowSchedulerCreationLocation",
                       "additionalConfigFileProviderList" : [configFile(fileId: "mule-runtime-maven-settings-MuleSettings", variable: "org.mule.maven.client.api.SettingsSupplierFactory.userSettings")],
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
