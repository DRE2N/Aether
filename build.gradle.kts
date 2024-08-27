import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    mavenLocal()
    maven("https://erethon.de/repo")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://ci.emc.gs/nexus/content/groups/aikar/")
    maven("https://repo.aikar.co/content/groups/aikar")
    maven("https://repo.md-5.net/content/repositories/releases/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://jitpack.io")
}

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "1.7.1"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.aether"
version = "1.0.0-SNAPSHOT"
description = "Mob and NPC plugin for Erethon"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21.1-R0.1-SNAPSHOT"

dependencies {
    paperweightDevBundle("de.erethon.papyrus", papyrusVersion) { isChanging = true }
    implementation("de.erethon:bedrock:1.4.0") { isTransitive = false }

    implementation("team.unnamed:hephaestus-api:0.11.1-dev-SNAPSHOT")
    implementation("team.unnamed:hephaestus-reader-blockbench:0.11.1-dev-SNAPSHOT")
    implementation("team.unnamed:hephaestus-runtime-bukkit-api:0.11.1-dev-SNAPSHOT")
    implementation("team.unnamed:hephaestus-runtime-bukkit-adapt:0.11.1-dev-SNAPSHOT")
    implementation("org.javassist:javassist:3.27.0-GA") // Needed for models
    compileOnly("de.erethon.hephaestus:Hephaestus:1.0-SNAPSHOT")

}

publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "Aether"
            version = "${project.version}"

            from(components["java"])
        }
    }
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
    }

    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        //uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:.*"))
            include(dependency("team.unnamed:.*:.*"))
            include(dependency("org.javassist:javassist:.*"))
        }
        relocate("de.erethon.bedrock", "de.erethon.aether.bedrock")
    }
    bukkit {
        main = "de.erethon.aether.Aether"
        apiVersion = "1.21"
        authors = listOf("Malfrador")
        load = BukkitPluginDescription.PluginLoadOrder.POSTWORLD
        commands {
            register("aether") {
                description = "Main command for Aether"
                aliases = listOf("ae", "mxl")
                permission = "aether.cmd"
                usage = "/aether help"
            }
        }
    }
}

