plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "0.5.0"
}
rootProject.name = "data-class-codec"
includeBuild("dcc")
includeBuild("test-plugin")
