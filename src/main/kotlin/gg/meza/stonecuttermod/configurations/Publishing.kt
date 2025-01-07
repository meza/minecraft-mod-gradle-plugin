package gg.meza.stonecuttermod.configurations

import dev.kikugie.stonecutter.build.StonecutterBuild
import gg.meza.stonecuttermod.mod
import gg.meza.stonecuttermod.upperCaseFirst
import org.gradle.api.Project
import me.modmuss50.mpp.ModPublishExtension
import net.fabricmc.loom.task.RemapJarTask
import org.gradle.kotlin.dsl.getByType
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType

fun configurePublishing(project: Project) {
    val stonecutter = project.extensions.getByType<StonecutterBuild>()
    val minecraftVersion = stonecutter.current.version
    val publishing = project.extensions.getByType(ModPublishExtension::class)
    val remapJar = project.tasks.named<RemapJarTask>("remapJar")
    val mod = project.mod

    publishing.apply {
        val isBeta = "next" in project.version.toString()
        changelog.set(
            project.rootProject.file("changelog.md").takeIf { it.exists() }?.readText() ?: "No changelog provided."
        )
        file.set(remapJar.get().archiveFile)
        version.set("${mod.version}+${mod.loader}-${minecraftVersion}")
        type.set(if (isBeta) BETA else STABLE)
        modLoaders.add(mod.loader)
        displayName.set("${mod.version} for ${mod.loader.upperCaseFirst()} $minecraftVersion")

        modrinth {
            accessToken.set(project.providers.environmentVariable("MODRINTH_TOKEN").orElse(""))
            projectId.set(project.providers.environmentVariable("MODRINTH_ID").orElse("0"))
            minecraftVersions.add(stonecutter.current.version)
            announcementTitle.set("Download ${mod.version}+${mod.loader}-${minecraftVersion}from Modrinth")
        }

        curseforge {
            projectSlug.set(project.providers.environmentVariable("CURSEFORGE_SLUG").orElse("server-redstone-block"))
            projectId.set(project.providers.environmentVariable("CURSEFORGE_ID").orElse("0"))
            accessToken.set(project.providers.environmentVariable("CURSEFORGE_TOKEN").orElse(""))
            minecraftVersions.add(stonecutter.current.version)
            announcementTitle.set("Download ${mod.version}+${mod.loader}-${minecraftVersion} from CurseForge")
        }

        dryRun.set(project.providers.environmentVariable("DO_PUBLISH").getOrElse("true").toBoolean())
    }
}
