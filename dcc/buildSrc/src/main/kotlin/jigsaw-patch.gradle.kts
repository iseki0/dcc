plugins {
    `java-library`
}


abstract class JP(private val project: Project) {
    fun enable(module: String){
        project.extensions.getByType(JavaPluginExtension::class.java).apply {
            modularity.inferModulePath = true
        }
        val sourceSets = project.sourceSets
        project.tasks.named("compileJava", JavaCompile::class.java) {
            options.compilerArgumentProviders.add(CommandLineArgumentProvider {
                // Provide compiled Kotlin classes to javac – needed for Java/Kotlin mixed sources to work
                listOf("--patch-module", "$module=${sourceSets["main"].output.asPath}")
            })
        }
        project.tasks.named("javadoc", Javadoc::class.java) {
            options {
                this as CoreJavadocOptions
                addStringOption("-patch-module", "$module=${sourceSets["main"].output.asPath}")
            }
        }
    }

}

extensions.create("jigsawPatch", JP::class.java, project)

