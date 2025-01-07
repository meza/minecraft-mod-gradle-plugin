package gg.meza.stonecuttermod.configurations

import dev.kikugie.stonecutter.build.StonecutterBuild
import gg.meza.stonecuttermod.mod
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType


fun configureDependencies(project: Project) {
    val stonecutter = project.extensions.getByType<StonecutterBuild>()
    val minecraftVersion = stonecutter.current.version

    project.dependencies.add("minecraft", "com.mojang:minecraft:$minecraftVersion")

    if(!project.mod.isNeoforge) {
        // normal projects
        project.dependencies.add("mappings", "net.fabricmc:yarn:${project.mod.prop("yarn_mappings")}:v2")
    }

    if (project.mod.isNeoforge) {
        // Neoforge is designed with mojmap in mind. When yarn is used, it needs to be patched.
        val loom = project.extensions.getByType(LoomGradleExtensionAPI::class)
        val neoforgeMappings = loom.layered {
            mappings("net.fabricmc:yarn:${project.mod.prop("yarn_mappings")}:v2")
            mappings("dev.architectury:yarn-mappings-patch-neoforge:${project.mod.prop("yarn_mappings_neoforge_patch")}")
        }
        project.dependencies.add("mappings", neoforgeMappings)
        project.dependencies.add("neoForge", "net.neoforged:neoforge:${project.mod.prop("neoforge_version")}")
    }

    if (project.mod.isForge) {
        project.dependencies.add("forge", "net.minecraftforge:forge:${project.mod.prop("forge_version")}")
    }

    if (project.mod.isFabric) {
        project.dependencies.add("modImplementation", "net.fabricmc:fabric-loader:${project.mod.prop("loader_version")}")
        project.dependencies.add("modApi", "net.fabricmc.fabric-api:fabric-api:${project.mod.prop("fabric_version")}")
        project.dependencies.add("modApi", "net.fabricmc.fabric-api:fabric-gametest-api-v1:${project.mod.prop("fabric_version")}")
    }



}
