package gg.meza.stonecraft.configurations

import gg.meza.stonecraft.IntegrationTest
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@DisplayName("Test the architectury quirks")
class QuirksTest : IntegrationTest {

    private lateinit var gradleTest: IntegrationTest.TestBuilder

    @BeforeEach
    fun setUp() {
        gradleTest = gradleTest().buildScript("""
tasks.register("printClasspathServer") {
    tasks.named<JavaExec>("runServer").get().classpath.forEach(${':'}${':'}println)
//    if (tasks.findByName("printClasspathTestServer") != null) {
        tasks.named<JavaExec>("runGameTestServer").get().classpath.forEach(${':'}${':'}println)
//    }
}
tasks.register("getForcedModules") {
    configurations.forEach {
        println(it.resolutionStrategy.forcedModules)
    }
}
""".trimIndent())
    }

    @Test
    fun `lwjgl is not present in the server`() {
        gradleTest.setStonecutterVersion("1.21.4", "fabric")
        val br = gradleTest.run("printClasspathServer")

        assertFalse(br.output.contains("org.lwjgl"), "LWJGL was not removed from the server classpath")
    }

    @Test
    fun `forge jopt is set properly`() {
        gradleTest.setStonecutterVersion("1.21.4", "forge")
        val br = gradleTest.run("getForcedModules")

        assertTrue(br.output.contains("net.sf.jopt-simple:jopt-simple:5.+"), "Jopt was not set properly for forge")
    }

}
