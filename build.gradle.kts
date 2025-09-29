plugins {
    kotlin("jvm") version "1.9.24"
    kotlin("plugin.serialization") version "1.9.24"
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.projectfilenotes"
version = "0.2.1"

repositories {
    mavenCentral()
}

kotlin {
    jvmToolchain(17)
}

intellij {
    version.set("2022.3")
    plugins.set(listOf("java", "Kotlin"))
}

dependencies {
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("com.vladsch.flexmark:flexmark-all:0.64.8")
}

tasks {
    patchPluginXml {
        sinceBuild.set("223")
        untilBuild.set("")
    }
    buildSearchableOptions {
        enabled = false
    }
    runIde {
        autoReloadPlugins.set(true)
    }
}