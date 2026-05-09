pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}

dependencyResolutionManagement {
    repositoriesMode = RepositoriesMode.FAIL_ON_PROJECT_REPOS
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "ubo-kotlin-apps"

// The phone-app and (future) wear-app modules live here. They consume the
// `:lib` (and its transitive `:protos`) from the sibling ubo-kotlin-grpc
// directory by re-projecting them into this build, so phone-app can depend
// on `project(":lib")` without a Maven publish step.
include(":phone-app")
include(":wear-app")
include(":widget-app")
include(":lib")
include(":protos")
project(":lib").projectDir = file("../ubo-kotlin-grpc/lib")
project(":protos").projectDir = file("../ubo-kotlin-grpc/protos")
