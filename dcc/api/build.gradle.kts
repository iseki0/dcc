plugins {
    convention
    `jigsaw-patch`
}

dependencies {
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains:annotations:24.1.0")
}

jigsawPatch {
    enable("space.iseki.dcc.api")
}

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xno-param-assertions")
    }
}

conv {
    mavenJava()
}
