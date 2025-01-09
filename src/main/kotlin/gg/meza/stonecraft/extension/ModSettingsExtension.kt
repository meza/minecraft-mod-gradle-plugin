package gg.meza.stonecraft.extension

import gg.meza.stonecraft.data.MinecraftClientOptions
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import javax.inject.Inject

abstract class ModSettingsExtension @Inject constructor(
    val project: Project
) {
    @Optional
    @get:Input
    val variableReplacements = project.objects.mapProperty(String::class.java, Any::class.java)

    @Internal
    @get:InputDirectory
    internal val generatedResourcesProp = project.objects.directoryProperty()

    @Internal
    @get:InputDirectory
    internal val runDirectoryProp = project.objects.directoryProperty()

    @get:Nested
    abstract val clientOptions: MinecraftClientOptions

    init {
        runDirectoryProp.convention(project.rootProject.layout.projectDirectory.dir("run"))
        generatedResourcesProp.convention(project.layout.projectDirectory.dir("src/main/generated"))
        variableReplacements.convention(mapOf())
    }

    var runDirectory: Directory
        get() = runDirectoryProp.get()
        set(value) {
            runDirectoryProp.set(value)
            runDirectoryProp.disallowChanges()
        }

    var generatedResources: Directory
        get() = generatedResourcesProp.get()
        set(value) {
            generatedResourcesProp.set(value)
            generatedResourcesProp.disallowChanges()
        }

    fun clientOptions(configure: MinecraftClientOptions.() -> Unit) {
        clientOptions.configure()
    }

}
