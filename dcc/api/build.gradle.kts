java {
    modularity.inferModulePath = true
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains:annotations:24.1.0")
}

tasks.named("compileJava", JavaCompile::class.java) {
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
        listOf("--patch-module", "space.iseki.dcc.api=${sourceSets["main"].output.asPath}")
    })
}

tasks.named("javadoc", Javadoc::class.java) {
    options {
        this as CoreJavadocOptions
        addStringOption("-patch-module", "space.iseki.dcc.api=${sourceSets["main"].output.asPath}")
    }
}
