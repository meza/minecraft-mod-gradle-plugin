package gg.meza.stonecraft

import org.gradle.api.Project

fun String.upperCaseFirst() = replaceFirstChar { if (it.isLowerCase()) it.uppercaseChar() else it }

fun getResourceVersionFor(version: String): Int {
    return when (version) {
        "1.20.2" -> 18
        "1.21" -> 34
        "1.21.4" -> 46
        else -> 18
    }
}


fun getProgramArgs(vararg lists: List<String>): List<String> {
    return lists.flatMap { it }
}

fun applyIfAbsent(pluginId: String, project: Project) {
    if(!project.pluginManager.hasPlugin(pluginId)) {
        project.pluginManager.apply(pluginId)
    }
}

enum class Side {
    CLIENT, SERVER
}