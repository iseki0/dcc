plugins {
    `java-gradle-plugin`
    convention
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
    publishing {
        // do afterEvaluate, otherwise name&description will miss
        afterEvaluate {
            publications.configureEach {
                this as MavenPublication
                pom { conv.applyConfiguration(this) }
            }
        }
    }
}
