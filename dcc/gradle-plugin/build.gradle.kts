plugins {
    kotlin("jvm")
    `java-gradle-plugin`
}


dependencies {
    implementation(kotlin("gradle-plugin-api"))
    implementation(project(":gen"))
}

gradlePlugin {
    plugins {
        create("postDcc") {
            id = "space.iseki.dcc.gradle-plugin"
            implementationClass = "space.iseki.dcc.gp.GPlugin"
        }
    }
}

afterEvaluate {
    signing {
        // To use local gpg command, configure gpg options in ~/.gradle/gradle.properties
        // reference: https://docs.gradle.org/current/userguide/signing_plugin.html#example_configure_the_gnupgsignatory
        useGpgCmd()
        for (it in publishing.publications) {
            sign(it)
        }
    }
}


