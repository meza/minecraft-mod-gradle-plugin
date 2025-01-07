package gg.meza.stonecuttermod

import org.junit.jupiter.api.Test


class BuildLogicTest : IntegrationTest {
    @Test
    fun testStuff() {
        val result = gradleTest().buildScript("").run("build")
        assert(result.output.contains("BUILD SUCCESSFUL"))
    }
}
