package gg.meza.stonecuttermod

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

enum class Side {
    CLIENT, SERVER
}
