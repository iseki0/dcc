# dcc

Work-In-Progress (Very early development)

Reflection-free utils for Kotlin data class and Java record.

## Gradle Plugin

The plugin do post-process on bytecodes by `doLast` in
every [`AbstractCompile`](https://docs.gradle.org/current/javadoc/org/gradle/api/tasks/compile/AbstractCompile.html)
and [`KotlinCompilationTask`](https://kotlinlang.org/docs/whatsnew18.html#gradle:~:text=org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask)
tasks.(afterEvaluated)