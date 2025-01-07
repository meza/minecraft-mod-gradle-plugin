package gg.meza.stonecuttermod.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import java.nio.file.Files
import kotlin.io.path.Path
import kotlin.io.path.createFile

/** Configures the Minecraft client
 *
 * Set some basic options on the minecraft client
 */
abstract class ConfigureMinecraftClient : DefaultTask() {
    @get:Input
    val runDirectory = project.objects.property(String::class.java)

    @get:Input
    val guiScale = project.objects.property(Int::class.java)

    @get:Input
    val fov: Property<Int> = project.objects.property(Int::class.java)

    @get:Input
    val narrator = project.objects.property(Boolean::class.java)

    @get:Input
    val musicVolume = project.objects.property(Double::class.java)

    @get:Input
    val darkBackground = project.objects.property(Boolean::class.java)

    @get:Input
    val additionalLines = project.objects.mapProperty(String::class.java, String::class.java)

    private var darkBackgroundValue: String
        get() = if (darkBackground.get()) "1" else "0"
        set(value) {
            darkBackground.set(value == "1")
        }

    private var narratorValue: String
        get() = if (narrator.get()) "1" else "0"
        set(value) {
            narrator.set(value == "1")
        }

    init {
        group = "minecraft"
        description = "Configures the Minecraft client"

        runDirectory.convention(project.rootProject.layout.projectDirectory.file("run").toString())
    }

    @TaskAction
    fun run() {
        val directory = Files.createDirectories(Path(runDirectory.get()))

        logger.lifecycle("Configuring Minecraft options in ${directory}...")
        val optionsFile = directory.resolve("options.txt")

        if (!Files.exists(optionsFile)) {
            optionsFile.createFile()
        }

        val optionsMap = mutableMapOf<String, String>()

        val optionsFileActual = optionsFile.toFile()

        if (optionsFileActual.length() > 0) {
            optionsFileActual.readLines().forEach { line ->
                val (key, value) = line.split(":")
                optionsMap[key] = value
            }
        }

        updateOption(optionsMap, "guiScale", guiScale)
        updateOption(optionsMap, "fov", fov)
        updateOption(optionsMap, "narrator", narratorValue)
        updateOption(optionsMap, "soundCategory_music", musicVolume)
        updateOption(optionsMap, "darkMojangStudiosBackground", darkBackgroundValue)
        updateOption(optionsMap, "joinedFirstServer", "true")
        updateOption(optionsMap, "lastServer", "127.0.0.1")

        additionalLines.get().forEach { (key, value) ->
            updateOption(optionsMap, key, value)
        }

        optionsFileActual.writeText(optionsMap.map { (key, value) -> "$key:$value" }.joinToString("\n"))

    }

    private fun updateOption(optionsMap: MutableMap<String, String>, key: String, value: String) {
        optionsMap[key] = value
    }

    private fun updateOption(optionsMap: MutableMap<String, String>, key: String, value: Property<*>) {
        if (!value.isPresent) {
            return
        }
        val v = value.get().toString()
        optionsMap[key] = if (v.contains(" ")) "\"$v\"" else v
    }

}
