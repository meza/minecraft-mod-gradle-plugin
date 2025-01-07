package gg.meza.stonecuttermod.configurations

import dev.kikugie.stonecutter.build.StonecutterBuild
import gg.meza.stonecuttermod.Side
import gg.meza.stonecuttermod.getProgramArgs
import gg.meza.stonecuttermod.mod
import net.fabricmc.loom.api.LoomGradleExtensionAPI
import net.fabricmc.loom.configuration.ide.RunConfigSettings
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

fun configureLoom(project: Project) {
    val loom = project.extensions.getByType(LoomGradleExtensionAPI::class)
    loom.apply {
        accessWidenerPath.set(project.rootProject.file("src/main/resources/${project.mod.id}.accesswidener"))

        if (project.mod.isForge) {
            forge.convertAccessWideners.set(true)
        }

        decompilers {
            getByName("vineflower").apply { options.put("mark-corresponding-synthetics", "1") }
        }

        runConfigs.all {
            isIdeConfigGenerated = true
            runDir = "../../run"
            if (environment == "client") programArgs("--username=developer")
        }
    }

    configureDatagen(project, loom)
    configureClientGameTests(project, loom)
    configureServerGameTests(project, loom)
}

/**
 * Configures the client game test tasks for the project
 */
fun configureClientGameTests(project: Project, loom: LoomGradleExtensionAPI) {
    val mod = project.mod

    /**
     * @TODO("Extract this to a common place")
     */
    val testClientDir = "../../run/testclient/${mod.loader}"

    loom.runs {
        create("gameTestClient") {
            client()
            runDir = testClientDir
            if (mod.isFabric) {
                fabricGameTestConfig(Side.CLIENT)
            }
            if (mod.isForgeLike) {
                forgeLikeConfig(Side.CLIENT)
            }
        }
    }
}

/**
 * Configures the client game test tasks for the project
 */
fun configureServerGameTests(project: Project, loom: LoomGradleExtensionAPI) {
    val mod = project.mod

    /**
     * @TODO("Extract this to a common place")
     */
    val testserverDir = "../../run/testserver/${mod.loader}"

    loom.runs {
        if (!(mod.isForge)) {
            create("gameTestServer") {
                server()
                runDir = testserverDir
                if (mod.isFabric) {
                    fabricGameTestConfig(Side.SERVER)
                }
                if (mod.isForgeLike) {
                   forgeLikeConfig(Side.SERVER)
                }
            }
        }
    }
}

/**
 * Configures the fabric game tests
 * @param side The side of the game test
 *
 */
private fun RunConfigSettings.fabricGameTestConfig(side: Side) {
    mapOf(
        "fabric-api.gametest" to "",
        "fabric-api.gametest.report-file" to project.rootProject.file("build/${side.name.lowercase()}-junit.xml").absolutePath
    ).forEach { (key, value) -> vmArg("-D$key=$value") }
}

/**
 * Configures the forge like game tests
 *
 * On the server side it also sets the `forge.gameTestServer` property to `true`
 * which is mostly undocumented and took a lot of debugging to figure out
 *
 * @param side The side of the game test
 */
private fun RunConfigSettings.forgeLikeConfig(side: Side) {
    mapOf(
        "forge.enabledGameTestNamespaces" to project.mod.id,
        "forge.enableGameTest" to "true"
    ).forEach { (key, value) -> property(key, value) }

    if (side == Side.SERVER) {
        property("forge.gameTestServer", "true")
    }
}

/**
 * Configures the datagen tasks for the project in regard to the loaders and the quirks of Architectury
 */
fun configureDatagen(project: Project, loom: LoomGradleExtensionAPI) {
    val stonecutter = project.extensions.getByType<StonecutterBuild>()
    val minecraftVersion = stonecutter.current.version

    val mod = project.mod
    val generatedResources = "src/main/generated"

    val modDefinition = listOf("--mod", mod.id)
    val generateAll = listOf("--all")
    val outputFolder = listOf("--output", project.file(generatedResources).absolutePath)
    val existingResources = listOf("--existing", project.rootProject.file("src/main/resources").absolutePath)

    val forgeLikeLogging: RunConfigSettings.() -> Unit = {
        mapOf(
            "${mod.loader}.logging.console.level" to "debug",
            "${mod.loader}.logging.markers" to "REGISTRIES"
        ).forEach { (key, value) -> property(key, value) }
    }

    loom.runs {

        if (mod.isForge && stonecutter.eval(minecraftVersion, "<1.21.4")) {
            // Forge 1.21.4 is currently broken with Architectury Loom 1.9.424
            // Once that gets fixed, the version condition above should be removed
            create("Datagen") {
                data()
                programArgs(getProgramArgs(generateAll, modDefinition, outputFolder, existingResources))
                forgeLikeLogging()
            }
        }

        if (mod.isNeoforge && false) {
            if (stonecutter.eval(minecraftVersion, ">=1.21.4")) {
                create("ServerDatagen") {
                    serverData()
                    programArgs(getProgramArgs(modDefinition, outputFolder))
                    forgeLikeLogging()
                }
                create("CliteDatagen") {
                    clientData()
                    programArgs(getProgramArgs(modDefinition, outputFolder))
                    forgeLikeLogging()
                }
            } else {
                create("Datagen") {
                    data()
                    programArgs(getProgramArgs(generateAll, modDefinition, outputFolder, existingResources))
                    forgeLikeLogging()
                }
            }
        }

    }

    // NeoForge changed their datagen approach in 1.21.4
    // This is disabled for now because Architectury Loom does not support it yet
    if (mod.isNeoforge && stonecutter.eval(stonecutter.current.version, ">=1.21.4")) {
//        project.tasks.register("runDatagen") {
//            dependsOn(project.tasks.named("runServerDatagen"), project.tasks.named("runClientDatagen"))
//        }
    }
}

