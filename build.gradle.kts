import net.minecrell.pluginyml.paper.PaperPluginDescription
import java.util.*
import java.text.SimpleDateFormat

plugins {
    id("java")
    id("maven-publish")
    id("net.kyori.blossom") version "2.2.0"
    id("com.gradleup.shadow") version ("9.3.1")
    id("de.eldoria.plugin-yml.paper") version ("0.8.0")
}

group = "eu.koolfreedom"
version = "4.2.5"
description = "KoolSMPCore"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.codemc.io/repository/maven-releases/")
    maven("https://repo.codemc.io/repository/maven-snapshots/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://jitpack.io/")
    maven("https://nexus.scarsz.me/content/groups/public/")
}

paper {
    name = rootProject.name.toString()
    version = project.version.toString()
    description = "Core plugin for KoolFreedomSMP"
    main = "eu.koolfreedom.KoolSMPCore"
    loader = "eu.koolfreedom.KoolLibraryManager"
    website = "https://github.com/KoolFreedom"
    authors = listOf("gamingto12", "0x7694C9", "videogamesm12", "sapph-ic")
    apiVersion = "1.21.10"
    generateLibrariesJson = true
    serverDependencies {
        register("LuckPerms") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Essentials") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("EssentialsDiscord") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("EssentialsDiscordLink") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("DiscordSRV") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("Vault") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
        register("packetevents") {
            required = false
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
        }
    }
}

dependencies {
    // Paper API
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Plugin integrations
    compileOnly("net.essentialsx:EssentialsX:2.20.1") {
        exclude("org.spigotmc", "spigot-api")
        exclude("org.bukkit", "bukkit")
    }
    compileOnly("net.essentialsx:EssentialsXDiscord:2.21.1") {
        exclude("org.spigotmc", "spigot-api")
    }
    compileOnly("net.essentialsx:EssentialsXDiscordLink:2.21.1") {
        exclude("org.spigotmc", "spigot-api")
    }
    implementation("com.github.LeonMangler:SuperVanish:6.2.18-3")
    compileOnly("net.luckperms:api:5.4")
    compileOnly("com.github.MilkBowl:VaultAPI:1.7.1") {
        exclude("org.bukkit", "bukkit")
    }
    implementation("com.github.retrooper:packetevents-spigot:2.11.2")

    // Utilities
    implementation("org.apache.commons:commons-lang3:3.18.0")
    implementation("org.projectlombok:lombok:1.18.30")
    annotationProcessor("org.projectlombok:lombok:1.18.30")
    implementation("org.reflections:reflections:0.10.2")
    compileOnly("net.dv8tion:JDA:5.0.0-beta.24") {
        exclude(group = "org.slf4j")
    }
    implementation("org.javassist:javassist:3.30.2-GA")
    implementation("com.google.code.gson:gson:2.10.1")

    // Integrations
    implementation("com.discordsrv:discordsrv:1.29.0")

    // Metrics
    implementation("org.bstats:bstats-bukkit:3.0.2")
    implementation("org.bstats:bstats-base:3.0.2")
}

tasks {
    // Update build.properties with build information
    processResources {
        val buildAuthor = project.findProperty("buildAuthor") ?: "KoolFreedom"
        val buildNumber = getBuildNumber()
        val buildDate = SimpleDateFormat("M/dd/yyyy 'at' h:mm:ss aa zzz").format(Date())

        inputs.property("buildAuthor", buildAuthor)
        inputs.property("buildNumber", buildNumber)
        inputs.property("buildVersion", project.version)
        inputs.property("buildDate", buildDate)

        filesMatching("build.properties") {
            expand(mapOf(
                "buildAuthor" to buildAuthor,
                "buildNumber" to buildNumber,
                "buildVersion" to project.version,
                "buildDate" to buildDate
            ))
        }
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    }

    shadowJar {
        archiveFileName.set("KoolSMPCore-${project.version}.jar")

        exclude("META-INF/*.SF")
        exclude("META-INF/*.DSA")
        exclude("META-INF/*.RSA")
        exclude("META-INF/versions/**")

        mergeServiceFiles()

        relocate("org.bstats", "eu.koolfreedom.libs.bstats")
        relocate("com.github.retrooper", "eu.koolfreedom.libs.packetevents")
        relocate("io.github.retrooper", "eu.koolfreedom.libs.packetevents")
        relocate("com.google.gson", "eu.koolfreedom.libs.gson")
        dependencies {
            include(dependency("org.bstats:.*"))
            include(dependency("org.reflections:.*"))
        }
    }

    build {
        dependsOn(shadowJar)
    }
}

/**
 * Read the current build number from build.properties and increment it.
 * If the file doesn't exist or the property isn't found, returns 1.
 */
fun getBuildNumber(): Int {
    val buildPropsFile = file("src/main/resources/build.properties")
    if (!buildPropsFile.exists()) {
        return 1
    }

    val props = mutableMapOf<String, String>()
    buildPropsFile.readLines().forEach { line ->
        if (line.isNotEmpty() && !line.startsWith("#")) {
            val (key, value) = line.split("=", limit = 2).let { parts ->
                if (parts.size == 2) parts[0] to parts[1] else return@forEach
            }
            props[key.trim()] = value.trim()
        }
    }
    val currentNumber = (props["buildNumber"] ?: "0").toIntOrNull() ?: 0
    return currentNumber + 1
}

/**
 * Task to increment build number in build.properties
 */
tasks.register("incrementBuildNumber") {
    doLast {
        val buildPropsFile = file("src/main/resources/build.properties")
        buildPropsFile.parentFile.mkdirs()

        val props = mutableMapOf<String, String>()
        if (buildPropsFile.exists()) {
            buildPropsFile.readLines().forEach { line ->
                if (line.isNotEmpty() && !line.startsWith("#")) {
                    val (key, value) = line.split("=", limit = 2).let { parts ->
                        if (parts.size == 2) parts[0] to parts[1] else return@forEach
                    }
                    props[key.trim()] = value.trim()
                }
            }
        }

        val currentNumber = (props["buildNumber"] ?: "0").toIntOrNull() ?: 0
        props["buildNumber"] = (currentNumber + 1).toString()
        props["buildVersion"] = project.version.toString()
        props["buildDate"] = System.currentTimeMillis().toString()

        buildPropsFile.writeText(props.entries.joinToString("\n") { (k, v) -> "$k=$v" })
    }
}
