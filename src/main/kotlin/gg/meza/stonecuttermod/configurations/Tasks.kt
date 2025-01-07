package gg.meza.stonecuttermod.configurations

import dev.kikugie.stonecutter.build.StonecutterBuild
import gg.meza.stonecuttermod.mod
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.register
import org.gradle.kotlin.dsl.withType

fun configureTasks(project: Project) {
    val stonecutter = project.extensions.getByType<StonecutterBuild>()
    val minecraftVersion = stonecutter.current.version
    val remapJar = project.tasks.named<RemapJarTask>("remapJar")

    val buildAndCollect = project.tasks.register<Copy>("buildAndCollect") {
        group = "build"
        from(remapJar.get().archiveFile)
        into(project.rootProject.layout.buildDirectory.file("libs"))
        dependsOn("build")
    }


    if (stonecutter.current.isActive) {
        project.rootProject.tasks.register("buildActive") {
            group = "mod"
            dependsOn(buildAndCollect)
        }
        project.rootProject.tasks.register("runActive") {
            group = "mod"
            dependsOn(project.tasks.named("runClient"))
        }

        project.rootProject.tasks.register("dataGenActive") {
            group = "mod"
            dependsOn(project.tasks.named("runDatagen"))
        }

        project.rootProject.tasks.register("testActiveClient") {
            group = "mod"
            dependsOn(project.tasks.named("runGameTestClient"))
        }

        if (!(project.mod.isForge && minecraftVersion.equals("1.21.4"))) {
            project.rootProject.tasks.register("testActiveServer") {
                group = "mod"
                dependsOn(project.tasks.named("runGameTestServer"))
            }
        }
    }
}
