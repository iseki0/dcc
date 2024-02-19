plugins {
    kotlin("jvm") version "1.9.22"
    id("com.github.johnrengelman.shadow") version "8.1.1" apply false
    `maven-publish`
    signing
}

allprojects {
    group = "space.iseki.dcc"
    version = "0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
    tasks.withType<AbstractArchiveTask> {
        isPreserveFileTimestamps = false
        isReproducibleFileOrder = true
    }
}


dependencies {
    testImplementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}

subprojects {
    apply<SigningPlugin>()
    apply<MavenPublishPlugin>()
    apply<JavaLibraryPlugin>()
    java {
        withJavadocJar()
        withSourcesJar()
    }
    publishing {
        repositories {
            maven {
                name = "Central"
                url = if (version.toString().endsWith("SNAPSHOT")) {
                    // uri("https://s01.oss.sonatype.org/content/repositories/snapshots")
                    uri("https://oss.sonatype.org/content/repositories/snapshots")
                } else {
                    // uri("https://s01.oss.sonatype.org/service/local/staging/deploy/maven2")
                    uri("https://oss.sonatype.org/service/local/staging/deploy/maven2")
                }
                credentials {
                    username = properties["ossrhUsername"]?.toString() ?: System.getenv("OSSRH_USERNAME")
                    password = properties["ossrhPassword"]?.toString() ?: System.getenv("OSSRH_PASSWORD")
                }
            }
        }
        publications {
            if (project.name!="gradle-plugin"){
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name = this@subprojects.name
                        description = "Reflection-free utils for Kotlin data class and Java record."
                        url = "https://github.com/iseki0/dcc"
                        licenses {
                            license {
                                name = "Apache-2.0"
                                url = "https://www.apache.org/licenses/LICENSE-2.0"
                            }
                        }
                        developers {
                            developer {
                                id = "iseki0"
                                name = "iseki zero"
                                email = "iseki@iseki.space"
                            }
                        }
                        scm {
                            connection = "scm:git:https://github.com/iseki0/dcc.git"
                            developerConnection = "scm:git:https://github.com/iseki0/dcc.git"
                            url = "https://github.com/iseki0/dcc"
                        }
                    }
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
    }
}

val publishAllToLocal: Task by tasks.creating {
    group = "publishing"
    for (it in subprojects) {
        dependsOn(it.tasks.publishToMavenLocal)
    }
}

val publishAll: Task by tasks.creating {
    group = "publishing"
    for (it in subprojects) {
        dependsOn(it.tasks.publish)
    }
}



