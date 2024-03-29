import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    `java-library`
    kotlin("jvm")
    `maven-publish`
    signing
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<AbstractArchiveTask> {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

java {
    withJavadocJar()
    withSourcesJar()
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xjvm-default=all")
        jvmTarget = JvmTarget.JVM_17
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
}

open class A(private val project: Project) {

    fun mavenJava(){
        project.extensions.getByType(PublishingExtension::class.java).apply {
            this.publications {
                create<MavenPublication>("mavenJava") {
                    from(project.components["java"])
                    pom { applyConfiguration(this) }
                }
            }
        }
    }

    fun applyConfiguration(pom:MavenPom) {
        pom.apply {
            name = project.name
            description = project.description
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

extensions.create("conv", A::class.java, project)
