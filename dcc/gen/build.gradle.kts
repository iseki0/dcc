dependencies {
    api(project(":api"))
    api("org.ow2.asm:asm:9.6")
    implementation("org.ow2.asm:asm-util:9.6")
    implementation("org.jetbrains.kotlinx:kotlinx-metadata-jvm:0.9.0")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
//    testImplementation("org.mockito:mockito-core:5.8.0")
}
dependencies {
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.named("compileJava", JavaCompile::class.java) {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "space.iseki.dcc.gen=${sourceSets["main"].output.asPath}")
    })
}

tasks.named("javadoc", Javadoc::class.java) {
    options {
        this as CoreJavadocOptions
        addStringOption("-patch-module", "space.iseki.dcc.gen=${sourceSets["main"].output.asPath}")
    }
}
