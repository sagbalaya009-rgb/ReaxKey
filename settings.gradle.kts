pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories { google(); mavenCentral() }
}
rootProject.name = "ReaxKey"
include(":app", ":ime", ":core:model", ":core:ui", ":feature:onboarding")
include(":core:data", ":core:search")
include(":core:media", ":feature:library", ":feature:settings")
include(":testbed")
