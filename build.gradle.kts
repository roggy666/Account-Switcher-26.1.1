/*
 * In-Game Account Switcher is a third-party mod for Minecraft Java Edition that
 * allows you to change your logged in account in-game, without restarting it.
 *
 * Copyright (C) 2015-2022 The_Fireplace
 * Copyright (C) 2021-2026 VidTu
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>
 */

// This is the main (multi-version loader) buildscript. It is processed by the
// Stonecutter multiple times, for each version and each loader. (compiled once)
// Based on Architectury Loom and processes the preparation/complation/building
// of the most of the mod that is not covered by the Stonecutter or Blossom.
// See "stonecutter.gradle.kts" for the Stonecutter configuration.
// See "settings.gradle.kts" for the Gradle configuration.

import com.google.gson.Gson
import com.google.gson.JsonElement
import net.fabricmc.loom.task.RemapJarTask
import net.fabricmc.loom.task.RunGameTask

plugins {
    alias(libs.plugins.architectury.loom)
}

// Extract the platform and Minecraft version.
val platform = "fabric"
val hackyNeoForge = false

// Extract versions.
val mc = sc.current
val mcv = mc.version // Literal version. (toString)
val mcp = mc.parsed // Comparable version. (operator overloading)

// Language.
// TODO(VidTu): Revisit after making the decision about 1.16.5/1.17.1 support.
val javaTarget = if (mcp >= "26.1") 25
else if (mcp >= "1.20.6") 21
else if (mcp >= "1.18.2") 17
else if (mcp >= "1.17.1") 16
else 8
val javaVersion = JavaVersion.toVersion(javaTarget)!!
java {
    sourceCompatibility = javaVersion
    targetCompatibility = javaVersion
    toolchain.languageVersion = JavaLanguageVersion.of(javaTarget)
}

// TODO(VidTu): When all legacy versions are done, the code from the legacy_shared module
// (and assets too) should be just fine to copy into the root code.
evaluationDependsOn(":legacy_shared") // Migration helper.
val legacyShared = project(":legacy_shared") // Migration helper.

// Metadata.
group = "ru.vidtu.ias"
base.archivesName = "IAS"
version = "${version}+${name}"
description = "Allows you to change which account you are signed in to in-game without restarting Minecraft."

// Define Stonecutter preprocessor variables/constants.
sc {
    constants["hacky_neoforge"] = hackyNeoForge
    constants {
        match(platform, "fabric", "forge", "neoforge")
    }
}

loom {
    // Prepare development environment.
    log4jConfigs.setFrom(rootDir.resolve("dev/log4j2.xml"))

    // Set up runs.
    runs {
        // Customize the client run.
        named("client") {
            // Set up debug VM args.
            // TODO(VidTu): Revisit after making the decision about 1.16.5/1.17.1 support.
            if (javaVersion.isJava9Compatible) {
                vmArgs("@../dev/args.vm.txt")
            } else {
                vmArgs(rootDir.resolve("dev/args.vm.txt")
                    .readLines()
                    .filter { "line.separator" !in it }
                    .filter { it.isNotBlank() })
            }

            // Set the run dir.
            runDir = "../../run"

            // AuthLib for 1.16.5 is bugged, disable Mojang API
            // to fix issues with multiplayer testing.
            if (mcp eq "1.16.5") { // TODO(VidTu): Revisit after making the decision about 1.16.5 support.
                vmArgs(
                    "-Dminecraft.api.auth.host=http://0.0.0.0:0/",
                    "-Dminecraft.api.account.host=http://0.0.0.0:0/",
                    "-Dminecraft.api.session.host=http://0.0.0.0:0/",
                    "-Dminecraft.api.services.host=http://0.0.0.0:0/",
                )
            }
        }

        // Remove server run, the mod is client-only.
        remove(findByName("server"))
    }

    // Configure Mixin.
    @Suppress("UnstableApiUsage") // <- Required to configure Mixin.
    mixin {
        // Use direct remapping instead of annotation processor and refmaps.
        useLegacyMixinAp = false
    }
}

// Make the game run with the compatible Java. (e.g,. Java 17 for 1.20.1)
tasks.withType<RunGameTask> {
    javaLauncher = javaToolchains.launcherFor(java.toolchain)
}

repositories {
    mavenCentral()
    maven("https://maven.fabricmc.net/") // Fabric.
    maven("https://maven.terraformersmc.com/releases/") // ModMenu.
}

dependencies {
    // Annotations.
    compileOnly(libs.jspecify)
    compileOnly(libs.jetbrains.annotations)
    compileOnly(libs.error.prone.annotations)
    testCompileOnly(libs.jspecify) // Migration helper.
    testCompileOnly(libs.jetbrains.annotations) // Migration helper.
    testCompileOnly(libs.error.prone.annotations) // Migration helper.

    // Minecraft. The dependency may be manually specified for example for snapshots.
    val minecraftDependencyProperty = findProperty("sc.minecraft-dependency")
    require(minecraftDependencyProperty != mcv) { "Unneeded 'sc.minecraft-dependency' property set to ${minecraftDependencyProperty} in ${project}, it already uses this version." }
    val minecraftDependency = minecraftDependencyProperty ?: mcv
    minecraft("com.mojang:minecraft:${minecraftDependency}")

    // Mappings not needed for 26.1+ (unobfuscated).

    // Force non-vulnerable Log4J, so that vulnerability scanners don't scream loud.
    // It's also cool for our logging config. (see the "dev/log4j2.xml" file)
    implementation(libs.log4j) {
        exclude("biz.aQute.bnd")
        exclude("com.github.spotbugs")
        exclude("org.osgi")
    }

    // Legacy Root migration
    compileOnly(legacyShared) // Migration helper.

    // Fabric Loader.
    implementation(libs.fabric.loader)

    // Fabric API.
    val fapi = "${property("sc.fabric-api")}"
    require(fapi.isNotBlank() && fapi != "[SC]") { "Fabric API version is not provided via 'sc.fabric-api' in ${project}." }
    implementation("net.fabricmc.fabric-api:fabric-api:${fapi}")

    // ModMenu.
    val modmenu = "${property("sc.modmenu")}"
    require(modmenu.isNotBlank() && modmenu != "[SC]") { "ModMenu version is not provided via 'sc.modmenu' in ${project}." }
    if ("${findProperty("sc.modmenu.compile-only")}".toBoolean()) {
        compileOnly("com.terraformersmc:modmenu:${modmenu}")
    } else {
        implementation("com.terraformersmc:modmenu:${modmenu}")
    }
}

// Compile with UTF-8, compatible Java, and with all debug options.
tasks.withType<JavaCompile> {
    source(legacyShared.sourceSets.main.get().java) // Migration helper.
    options.encoding = "UTF-8"
    options.compilerArgs.addAll(listOf("-g", "-parameters"))
    // JDK 8 (used by 1.16.x) doesn't support the "-release" flag and
    // uses "-source" and "-target" ones (see the top of the file),
    // so we must NOT specify it, or the "javac" will fail.
    // JDK 9+ does listen to this option.
    // TODO(VidTu): Revisit after making the decision about 1.16.5/1.17.1 support.
    if (javaVersion.isJava9Compatible) {
        options.release = javaTarget
    }
}

tasks.withType<ProcessResources> {
    from(legacyShared.sourceSets.main.get().resources) // Migration helper.
    // Filter with UTF-8.
    filteringCharset = "UTF-8"

    // Exclude not needed loader entrypoint files.
    exclude("META-INF/mods.toml", "META-INF/neoforge.mods.toml")

    // Platform requirement is not used for Fabric builds.

    // Expand Minecraft requirement that can be manually overridden for reasons. (e.g., snapshots)
    val minecraftRequirementProperty = findProperty("sc.minecraft-requirement")
    require(minecraftRequirementProperty != mcv) { "Unneeded 'sc.minecraft-requirement' property set to ${minecraftRequirementProperty} in ${project}, it already uses this version." }
    val minecraftRequirement = minecraftRequirementProperty ?: mcv
    inputs.property("minecraft", minecraftRequirement)

    val mixinJava = javaTarget
    inputs.property("mixinJava", mixinJava)

    // Expand version and dependencies.
    inputs.property("version", version)
    inputs.property("platform", platform)
    filesMatching(listOf("fabric.mod.json", "quilt.mod.json", "ias.mixins.json", "META-INF/mods.toml", "META-INF/neoforge.mods.toml")) {
        expand(inputs.properties)
    }

    // Minify JSON (including ".mcmeta") and TOML files.
    val files = fileTree(outputs.files.asPath)
    doLast {
        val jsonAlike = Regex("^.*\\.(?:json|mcmeta)$", RegexOption.IGNORE_CASE)
        files.forEach {
            if (it.name.matches(jsonAlike)) {
                it.writeText(Gson().fromJson(it.readText(), JsonElement::class.java).toString())
            } else if (it.name.endsWith(".toml", ignoreCase = true)) {
                it.writeText(it.readLines()
                    .filter { s -> !s.startsWith('#') }
                    .filter { s -> s.isNotBlank() }
                    .joinToString("\n")
                    .replace(" = ", "="))
            }
        }
    }
}

// Add LICENSE and manifest into the JAR file.
// Manifest also controls Mixin/mod loading on some loaders/versions.
tasks.withType<Jar> {
    // Add LICENSE, GPL (reference for LICENSE), and NOTICE.
    from(rootDir.resolve("GPL"))
    from(rootDir.resolve("LICENSE"))
    from(rootDir.resolve("NOTICE"))

    // Remove package-info.class, unless package debug is on. (to save space)
    if (!"${findProperty("ru.vidtu.ias.debug.package")}".toBoolean()) {
        exclude("**/package-info.class")
    }

    // Add manifest.
    manifest {
        attributes(
            "Specification-Title" to "In-Game Account Switcher",
            "Specification-Version" to version,
            "Specification-Vendor" to "VidTu",
            "Implementation-Title" to "IAS",
            "Implementation-Version" to version,
            "Implementation-Vendor" to "VidTu",
            "MixinConfigs" to "ias.mixins.json" // Forge and old NeoForge.
        )
    }
}

// Output into "build/libs" instead of "versions/<ver>/build/libs".
tasks.withType<RemapJarTask> {
    destinationDirectory = rootProject.layout.buildDirectory.file("libs").get().asFile
}
