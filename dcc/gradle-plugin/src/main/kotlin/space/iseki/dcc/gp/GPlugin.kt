package space.iseki.dcc.gp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import space.iseki.dcc.gen.PostProcess
import java.io.File

/**
 * Gradle plugin DCC
 */
@Suppress("unused")
class GPlugin : Plugin<Project> {
    /**
     * Apply the plugin to the given target object.
     */
    override fun apply(target: Project) {
        @Suppress("LocalVariableName") val LOG = target.logger
        LOG.info("DCC plugin apply")
        target.afterEvaluate { project ->
            for (task in project.tasks.filter { it is AbstractCompile || it is KotlinCompilationTask<*> }) {
                LOG.info("register to task: {}", task)
                task.doLast {
                    LOG.info("begin post-process for task: {}", task)
                    for (dir in it.outputs.files.files.filter(File::isDirectory)) {
                        LOG.info("post-processing dir: {}", dir)
                        PostProcess.processDir(dir)
                    }
                }
            }
        }
    }

}

