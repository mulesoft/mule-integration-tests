def UPSTREAM_PROJECTS_LIST = [ "Mule-runtime/mule/4.3.0-JANUARY-2022" ]

Map pipelineParams = [ "upstreamProjects" : UPSTREAM_PROJECTS_LIST.join(','),
                       // MULE-18045: Comment public setting to get raml-parser 2 from private repo until it is released in a public repo
                       // "mavenSettingsXmlId" : "mule-runtime-maven-settings-MuleSettings",
                       "mavenAdditionalArgs" : "-Dmule.scheduler.alwaysShowSchedulerCreationLocation",
                       "additionalConfigFileProviderList" : [configFile(fileId: "mule-runtime-maven-settings-MuleSettings", variable: "org.mule.maven.client.api.SettingsSupplierFactory.userSettings")],
                       "projectType" : "Runtime" ]

runtimeBuild(pipelineParams)
