package space.iseki.dcc.gp

import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.compile.AbstractCompile
import org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask
import space.iseki.dcc.gen.PostProcess

class GPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        val LOG = target.logger
        LOG.info("DCC plugin apply")
        target.afterEvaluate { project ->
            for (task in project.tasks.filter { it is AbstractCompile || it is KotlinCompilationTask<*> }) {
                LOG.info("register to task: {}", task)
                task.doLast {
                    LOG.info("begin post-process for task: {}", task)
                    for (dir in it.outputs.files.files.filter { it.isDirectory }) {
                        LOG.info("post-processing dir: {}", dir)
                        PostProcess.processDir(dir)
                    }
                }
            }
        }
    }

}

