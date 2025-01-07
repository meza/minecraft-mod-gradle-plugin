package gg.meza.stonecuttermod.tasks

import gg.meza.stonecuttermod.IntegrationTest
import org.gradle.api.Project
import org.gradle.kotlin.dsl.register
import org.junit.jupiter.api.*
import org.junit.jupiter.api.Assertions.*
import java.io.File

class ConfigureMinecraftClientTest: IntegrationTest {

    private lateinit var testBuilder: IntegrationTest.TestBuilder
    private lateinit var project: Project
    private lateinit var optionsFile: File

    @BeforeEach
    fun setUp() {
        testBuilder = gradleTest()
        project = testBuilder.project()

        optionsFile = project.rootProject.file("run/options.txt")
        optionsFile.parentFile.mkdirs()
    }

    @Test
    fun testWhenOptionsDontExist() {
        if(optionsFile.exists()) {
            optionsFile.delete()
        }

        val task = project.tasks.register<ConfigureMinecraftClient>("configureMinecraftClient") {
            guiScale.set(2)
            fov.set(80)
            narrator.set(true)
            musicVolume.set(0.5)
            darkBackground.set(false)
            additionalLines.set(mapOf("someKey" to "someValue"))
        }.get()

        task.run()

        val outputFileContents = optionsFile.readText()
        assertTrue(optionsFile.exists(), "The options.txt file should be created.")
        assertTrue(outputFileContents.contains("guiScale:2"), "The guiScale option should be set to 2 but is not.")
        assertTrue(outputFileContents.contains("fov:80"), "The fov option should be set to 80 but is not.")
        assertTrue(outputFileContents.contains("narrator:1"), "The narrator option should be set to true but is not.")
        assertTrue(outputFileContents.contains("music:0.5"), "The music option should be set to 0.5 but is not.")
        assertTrue(outputFileContents.contains("darkMojangStudiosBackground:0"), "The dark_background option should be set to false but is not.")
        assertTrue(outputFileContents.contains("someKey:someValue"), "The additionalLines option should be set to someKey:someValue but is not.")
    }

    @Test
    fun testWhenThereAreExistingOptions() {
        if(optionsFile.exists()) {
            optionsFile.delete()
        }

        optionsFile.writeText("guiScale:1\nfov:70\nnarrator:false\nmusic:0.0\ndark_background:true\nexistingKey:existingValue")

        val task = project.tasks.register<ConfigureMinecraftClient>("configureMinecraftClient") {
            guiScale.set(2)
            narrator.set(true)
            musicVolume.set(0.5)
            darkBackground.set(false)
            additionalLines.set(mapOf("someKey" to "someValue"))
        }.get()

        task.run()

        val outputFileContents = optionsFile.readText()
        assertTrue(optionsFile.exists(), "The options.txt file should be created.")
        assertTrue(outputFileContents.contains("guiScale:2"), "The guiScale option should be set to 2 but is not.")
        assertTrue(outputFileContents.contains("fov:70"), "The fov option should be the original but is not.")
        assertTrue(outputFileContents.contains("narrator:1"), "The narrator option should be set to true but is not.")
        assertTrue(outputFileContents.contains("music:0.5"), "The music option should be set to 0.5 but is not.")
        assertTrue(outputFileContents.contains("darkMojangStudiosBackground:0"), "The dark_background option should be set to false but is not.")
        assertTrue(outputFileContents.contains("someKey:someValue"), "The additionalLines option should be set to someKey:someValue but is not.")
        assertTrue(outputFileContents.contains("existingKey:existingValue"), "Original settings should be there but are not.")
    }
}
