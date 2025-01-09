package gg.meza.stonecraft.configurations

import gg.meza.stonecraft.mod
import org.gradle.api.Project
import org.gradle.api.tasks.JavaExec
import org.gradle.kotlin.dsl.named

fun patchAroundArchitecturyQuirks(project: Project) {
    addForgeJOPTDependency(project)
    removeUnnecessaryLWJGLDependencies(project)
}

/**
 * Architectury loom is adding LWGJL natives but not the actual LWJGL dependency causing Forge to not boot
 * We are removing the natives from the classpath from the SERVER because it's not needed
 *
 * @see https://github.com/architectury/architectury-loom/issues/191#issuecomment-2030567486
 */
private fun removeUnnecessaryLWJGLDependencies(project: Project) {
    project.afterEvaluate {
        tasks.named<JavaExec>("runServer") {
            classpath = classpath.filter { !it.toString().contains("\\org.lwjgl\\") }
        }

        if (tasks.findByName("runGameTestServer") != null) {
            tasks.named<JavaExec>("runGameTestServer") {
                classpath = classpath.filter { !it.toString().contains("\\org.lwjgl\\") }
            }
        }
    }
}

/**
 * @see https://discord.com/channels/792699517631594506/792701725106634783/1272848116864909314
 */
private fun addForgeJOPTDependency(project: Project) {
    if (project.mod.isForge) {
        project.configurations.configureEach {
            resolutionStrategy.force("net.sf.jopt-simple:jopt-simple:5.+")
        }
    }
}
