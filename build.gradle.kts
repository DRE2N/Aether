import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    maven("https://erethon.de/repo")
    maven("https://repo.dmulloy2.net/repository/public/")
}
plugins {
    `java-library`
    id("io.papermc.paperweight.userdev") version "1.3.3"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("com.github.johnrengelman.shadow") version "7.1.2"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.aether"
version = "1.0.0-SNAPSHOT"
description = "Mob and NPC plugin for erethon"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}

dependencies {
    paperDevBundle("1.18.1-R0.1-SNAPSHOT")
    implementation("de.erethon.commons:commons-dist:6.3.3")
    compileOnly("com.comphenix.protocol:ProtocolLib:4.8.0-SNAPSHOT")
    // paperweightDevBundle("com.example.paperfork", "1.18.1-R0.1-SNAPSHOT")

    // You will need to manually specify the full dependency if using the groovy gradle dsl
    // (paperDevBundle and paperweightDevBundle functions do not work in groovy)
    // paperweightDevelopmentBundle("io.papermc.paper:dev-bundle:1.18.1-R0.1-SNAPSHOT")
}

tasks {
    // Configure reobfJar to run when invoking the build task
    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything

        // Set the release flag. This configures what version bytecode the compiler will emit, as well as what JDK APIs are usable.
        // See https://openjdk.java.net/jeps/247 for more information.
        options.release.set(17)
    }
    javadoc {
        options.encoding = Charsets.UTF_8.name() // We want UTF-8 for everything
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name() // We want UTF-8 for everything
    }

    /*
    reobfJar {
      // This is an example of how you might change the output location for reobfJar. It's recommended not to do this
      // for a variety of reasons, however it's asked frequently enough that an example of how to do it is included here.
      outputJar.set(layout.buildDirectory.file("libs/PaperweightTestPlugin-${project.version}.jar"))
    }
     */

    shadowJar {
        dependencies {
            include(dependency("de.erethon.commons:commons-dist:6.3.3"))
            include(dependency("com.github.retrooper:packetevents:v1.8-pre-19"))
        }
        relocate("de.erethon.commons", "de.erethon.aether.commons")
        relocate("com.github.retrooper", "de.erethon.aether.packetevents")
    }
    bukkit {
        load = BukkitPluginDescription.PluginLoadOrder.STARTUP
        main = "de.erethon.aether.Aether"
        apiVersion = "1.18"
        authors = listOf("Malfrador")
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

