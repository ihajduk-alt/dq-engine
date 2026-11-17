rootProject.name = "dq-engine-build"

dependencyResolutionManagement {
    repositories {
        mavenCentral()
    }
}

include("dq-engine", "dq-example-http")
