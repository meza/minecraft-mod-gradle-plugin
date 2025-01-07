package gg.meza.stonecuttermod

import dev.kikugie.stonecutter.AnyVersion
import dev.kikugie.stonecutter.build.StonecutterBuild
import gg.meza.stonecuttermod.configurations.configureDependencies
import gg.meza.stonecuttermod.configurations.configureLoom
import gg.meza.stonecuttermod.configurations.configureTasks
import gg.meza.stonecuttermod.tasks.McMetaCreation
import net.fabricmc.loom.configuration.FabricApiExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.DuplicatesStrategy
import org.gradle.api.plugins.BasePluginExtension
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.JavaExec
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.*
import org.gradle.language.jvm.tasks.ProcessResources
import java.util.*


class ModPlugin : Plugin<Project> {

    private fun initialiseProject(project: Project) {
        project.extra["loom.platform"] = project.mod.loader
        project.group = project.mod.group;

        project.plugins.apply("idea")
        project.plugins.apply("java")
        project.plugins.apply("dev.architectury.loom")
        project.plugins.apply("me.modmuss50.mod-publish-plugin")
    }

    override fun apply(project: Project) {
        val extension = project.extensions.create("modSettings", ModExtension::class.java)

        project.afterEvaluate {
            initialiseProject(project)

            val stonecutter = project.extensions.getByType<StonecutterBuild>()
            val generatedResources = "src/main/generated"

            project.base().archivesName.set("${project.mod.id}-${project.mod.version}")

            val minecraftVersion = stonecutter.current.version
            project.version = "${project.mod.version}+mc${minecraftVersion}"


            // Load version specific dependencies from versions/dependencies/[minecraftVersion].properties
            loadVersionSpecificDependencies(project, minecraftVersion)

            // Set the basic repositories for a multiloader project
            project.repositories {
                mavenCentral()
                maven("https://maven.fabricmc.net/")
                maven("https://maven.architectury.dev")
                maven("https://maven.minecraftforge.net")
                maven("https://maven.neoforged.net/releases/")
            }

            configureDependencies(project)

            // Set the stonecutter constants for platform detection.
            // This is useful for conditional dependencies in the code (not in the buildscript)
            stonecutter.consts(
                Pair("fabric", project.mod.loader == "fabric"),
                Pair("forge", project.mod.loader == "forge"),
                Pair("neoforge", project.mod.loader == "neoforge"),
                Pair("quilt", project.mod.loader == "quilt"),
                Pair("forgeLike", project.mod.isForgeLike),
                Pair("fabricLike", project.mod.isFabricLike),
            )

            //Configure the compile time
            project.project.configure<JavaPluginExtension> {
                // Configure the Java plugin to use the correct Java version for the given Minecraft version
                val javaVersion = javaVersion(stonecutter)
                sourceCompatibility = javaVersion
                targetCompatibility = javaVersion
                toolchain.languageVersion.set(JavaLanguageVersion.of(javaVersion.toString()))

                // Add the generated resources directory to the resources source set for ForgeLike mods
                // This is to allow them to read the generated resources
                if (project.mod.isForgeLike) {
                    sourceSets.named("main").get().resources.srcDir(generatedResources)
                }
            }

            // There are a few bits that need to be patched around Architectury
            patchAroundArchitecturyQuirks(project)

            if(project.mod.isForge) {
                project.tasks.register<McMetaCreation>("generatePackMCMetaJson") {
                    resourcePackVersion = getResourceVersionFor(minecraftVersion)
                }
            }

            // Deal with the general resources
            project.project.tasks.withType<ProcessResources> {
                duplicatesStrategy = DuplicatesStrategy.INCLUDE
                val currentResourceVersion = getResourceVersionFor(minecraftVersion)
                val needsOldResources =
                    currentResourceVersion < 34; //Version 43 changed how the resource directories are named

                doFirst {
                    println(String.format("Current resource version is %d", currentResourceVersion))
                }

                val basicModDetails = mapOf(
                    "id" to project.mod.id,
                    "name" to project.mod.name,
                    "description" to project.mod.description,
                    "version" to project.mod.version,
                    "minecraftVersion" to minecraftVersion,
                    "packVersion" to getResourceVersionFor(minecraftVersion),
                    "fabricVersion" to project.mod.prop("fabric_version"),
                    "forgeVersion" to project.mod.prop("forge_version"),
                    "neoforgeVersion" to project.mod.prop("neoforge_version", "not set"),
                )
                filesMatching(listOf("**/*.json", "**/*.toml", "**/*.mcmeta")) { expand(basicModDetails) }

                // Exclude the correct mod metadata file based on the loader
                when {
                    project.mod.isFabric -> {
                        exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml", "pack.mcmeta")
                    }

                    project.mod.isNeoforge -> {
                        exclude("fabric.mod.json", "META-INF/mods.toml", "pack.mcmeta")
                    }

                    project.mod.isForge -> {
                        exclude("fabric.mod.json", "META-INF/neoforge.mods.toml")
                    }
                }

                // If the mod is using the old resource system, we need to change the item identifier
                // Ideally you're using data generators and this is not an issue for you
                filesMatching("data/**/*.json") {
                    expand(
                        mapOf(
                            "itemIdentifier" to if (needsOldResources) "item" else "id"
                        )
                    )
                }

                // Similarly, if the mod is using the old resource system, we need to rename some directories
                // This is because the old system used a different naming convention
                // Ideally you're using generators and this is not an issue.
                // The Structures directory might be a problem as that's not something you usually generate
                if (needsOldResources) {
                    println("Using old resource system")

                    val renameMappings = mapOf(
                        "data/minecraft/tags/block" to "data/minecraft/tags/blocks",
                        "data/${project.mod.id}/advancement/recipe" to "data/${project.mod.id}/advancement/recipes",
                        "data/${project.mod.id}/advancement" to "data/${project.mod.id}/advancements",
                        "data/${project.mod.id}/loot_table" to "data/${project.mod.id}/loot_tables",
                        "data/${project.mod.id}/recipe" to "data/${project.mod.id}/recipes",
                        "data/${project.mod.id}/structure" to "data/${project.mod.id}/structures"
                    )

                    renameMappings.forEach { (source, destination) ->
                        filesMatching("$source/**") {
                            path = path.replace(source, destination)
                        }
                    }
                }

                if (project.mod.isForge) {
                    finalizedBy("generatePackMCMetaJson")
                }

                doLast {
                    // Process the pack.mcmeta file for Forge
                    cleanUpEmptyResourceDirectories(project)
                }
            }

            if (project.mod.isFabric) {
                project.fabricApi().apply {
                    configureDataGeneration {
                        if (stonecutter.eval(stonecutter.current.version, ">=1.21.4")) {
                            client = true
                        }
                        outputDirectory.set(project.file(generatedResources))
                    }
                }
            }

            configureLoom(project)
            configureTasks(project)
        }
    }


    private fun Project.base(): BasePluginExtension {
        return extensions.getByType(BasePluginExtension::class)
    }

    private fun Project.fabricApi(): FabricApiExtension {
        return extensions.getByType(FabricApiExtension::class)
    }

    private fun cleanUpEmptyResourceDirectories(project: Project) {
        val buildResourcesDir = project.layout.buildDirectory.dir("resources/main").get().asFile

        if (buildResourcesDir.exists()) {
            println("Cleaning up empty directories in ${buildResourcesDir.path}")

            // Recursively delete empty directories
            buildResourcesDir.walkBottomUp().filter { it.isDirectory && it.listFiles().isNullOrEmpty() }
                .forEach { dir ->
                    dir.delete()
                }
        }
    }

    private fun patchAroundArchitecturyQuirks(project: Project) {

        // Forge needs this dependency to be forced to a newer version
        if (project.mod.isForge) {
            project.configurations.configureEach {
                resolutionStrategy.force("net.sf.jopt-simple:jopt-simple:5.+")
            }
        }

        // Fix for an Architectury issue with LWJGL
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
     * Load version specific dependencies from a properties file
     * It will look for a file in the versions/dependencies directory with the name of the minecraft version
     *
     * @param project The project to load the dependencies into
     * @param minecraftVersion The version of Minecraft to load the dependencies for
     *
     */
    private fun loadVersionSpecificDependencies(project: Project, minecraftVersion: AnyVersion) {
        val customPropsFile = project.rootProject.file("versions/dependencies/${minecraftVersion}.properties")

        if (customPropsFile.exists()) {
            val customProps = Properties().apply {
                customPropsFile.inputStream().use { load(it) }
            }
            customProps.forEach { key, value ->
                project.extra[key.toString()] = value
            }
        }
    }

    private fun javaVersion(stonecutter: StonecutterBuild): JavaVersion {
        val j21 = stonecutter.eval(stonecutter.current.version, ">=1.20.6")
        return if (j21) JavaVersion.VERSION_21 else JavaVersion.VERSION_17
    }
}

