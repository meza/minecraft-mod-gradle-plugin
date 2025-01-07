import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetHierarchy.SourceSetTree.Companion.test
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-gradle-plugin`
    `kotlin-dsl`
    kotlin("jvm") version "2.+"
    id("com.gradle.plugin-publish") version "1.+"
}

group = "gg.meza.stonecuttermod"

repositories {
    mavenCentral()
    mavenLocal()
    gradlePluginPortal()
    maven("https://maven.kikugie.dev/releases")
    maven("https://maven.fabricmc.net/")
    maven("https://maven.architectury.dev")
    maven("https://maven.minecraftforge.net")
    maven("https://maven.neoforged.net/releases/")
}

dependencies {
    compileOnly(gradleApi())
    fun plugin(id: String, version: String) = "${id}:${id}.gradle.plugin:${version}"
    implementation(plugin("dev.kikugie.stonecutter", "0.5"))
    implementation("com.google.code.gson:gson:2.+")
    implementation(plugin("dev.architectury.loom", "1.9.+"))
    implementation(plugin("me.modmuss50.mod-publish-plugin", "0.+"))

    testImplementation(gradleTestKit())
    testImplementation("org.junit.jupiter:junit-jupiter:5.+")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.named<Test>("test") {
    useJUnitPlatform()
}

java {
    withSourcesJar()
}

gradlePlugin {
    plugins {
        create("gg.meza.stonecuttermod") {
            id = "gg.meza.stonecuttermod"
            displayName = "Meza's Stonecutter Mod defaults"
            implementationClass = "gg.meza.stonecuttermod.ModPlugin"
            version = "0.002"
        }
    }
}

val compileKotlin: KotlinCompile by tasks

compileKotlin.compilerOptions {
    languageVersion.set(org.jetbrains.kotlin.gradle.dsl.KotlinVersion.KOTLIN_2_0)
    jvmTarget.set(JvmTarget.JVM_21)
    freeCompilerArgs.addAll(listOf("-opt-in=kotlin.ExperimentalStdlibApi", "-opt-in=kotlin.RequiresOptIn"))
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
    targetCompatibility = JavaVersion.VERSION_21
}
