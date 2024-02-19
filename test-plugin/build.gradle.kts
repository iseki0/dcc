plugins{
    kotlin("jvm") version "1.9.22"
    id("space.iseki.dcc.gradle-plugin") version "0.1-SNAPSHOT"
}

allprojects {
    version = "0.1-SNAPSHOT"
    repositories {
        mavenCentral()
    }
    tasks.withType<Test> {
        useJUnitPlatform()
    }
}

dependencies {
    implementation("space.iseki.dcc:gen")
    implementation("space.iseki.dcc:api")
    implementation("org.jetbrains.kotlin:kotlin-test")
}

kotlin {
    jvmToolchain(17)
}
