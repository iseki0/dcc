plugins {
    kotlin("jvm")
}

dependencies {
    api(project(":api"))
    api("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
//    testImplementation("org.mockito:mockito-core:5.8.0")
}
