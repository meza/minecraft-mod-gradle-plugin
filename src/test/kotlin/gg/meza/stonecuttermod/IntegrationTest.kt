package gg.meza.stonecuttermod

import org.gradle.api.Project
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import java.io.File


interface IntegrationTest {
    companion object {
        @Language("gradle")
        val kotlinHeader = """
            import gg.meza.stonecuttermod.mod
            
            plugins {
                id("gg.meza.stonecuttermod")
            }
        """.trimIndent()
    }

    class TestBuilder {
        private val runner = GradleRunner.create()
            .withPluginClasspath()
            .forwardOutput()
            .withDebug(true)

        private val gradleHome: File
        private val projectDir: File
        private val buildScript: File
        private val gradleSettings: File
        private val gradleProperties: File
        private var arguments = ArrayList<String>()

        init {
            val testDir = File("build/e2e_tests")
            val ext = ".kts"
            gradleHome = File(testDir, "home")
            projectDir = File(testDir, "project")
            buildScript = File(projectDir, "build.gradle$ext")
            gradleSettings = File(projectDir, "settings.gradle$ext")
            gradleProperties = File(projectDir, "gradle.properties")

            projectDir.mkdirs()

            // Clean up
            File(projectDir, "build.gradle").delete()
            File(projectDir, "build.gradle.kts").delete()
            File(projectDir, "settings.gradle").delete()
            File(projectDir, "settings.gradle.kts").delete()
            File(projectDir, "gradle.properties").delete()

            val resources = File(projectDir, "src/main/resources")
            resources.mkdirs()
            File(resources, "fabric.mod.json").writeText("{}")

            buildScript.appendText(kotlinHeader)

            gradleSettings.writeText("rootProject.name = \"example-mod\"")

            runner.withProjectDir(projectDir)
            arguments.addAll(
                listOf(
                    "--gradle-user-home", gradleHome.absolutePath,
                    "--stacktrace",
                    "--warning-mode", "fail",
                    "clean"
                )
            )
        }

        fun withProperties(properties: Map<String, String>): TestBuilder {
            properties.forEach { (key, value) ->
                gradleProperties.appendText("$key=$value\n")
            }
            return this
        }

        fun buildScript(@Language("gradle") script: String): TestBuilder {
            buildScript.appendText(script + "\n")
            return this
        }

        fun project(): Project {
            return ProjectBuilder.builder()
                .withProjectDir(projectDir)
                .withName("example-mod")
                .build()
        }

        fun run(task: String): BuildResult {
            arguments.add(task)
            runner.withArguments(arguments)
            return runner.run()
        }
    }
    fun gradleTest(): TestBuilder {
        return TestBuilder()
    }
}

fun Project.setProperties(properties: Map<String, String>) {
    properties.forEach { (key, value) ->
        extensions.extraProperties[key] = value
    }
}
