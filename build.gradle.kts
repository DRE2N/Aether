import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    mavenLocal()
    maven("https://repo.erethon.de/snapshots/")
}

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("xyz.jpenilla.run-paper") version "2.3.1"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.aether"
version = "1.0.1-SNAPSHOT"
description = "Mob and NPC plugin for Erethon"

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21.11-R0.1-SNAPSHOT"
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweightDevelopmentBundle("de.erethon.papyrus", "dev-bundle", papyrusVersion)

    compileOnly("de.erethon", "Daedalus", "1.4-SNAPSHOT")

    compileOnly("de.erethon.hephaestus:Hephaestus:1.0.6-SNAPSHOT")
    compileOnly("de.erethon.questsxl:QuestsXL:1.0.5-SNAPSHOT")
    compileOnly("de.erethon.hecate:Hecate:1.2-SNAPSHOT")
    compileOnly("de.erethon.factions:Factions:1.0-SNAPSHOT")
}

tasks {
    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        //uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        runDirectory.set(file("C:\\Dev\\Erethon"))
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    val sourcesJar by creating(Jar::class) {
        archiveClassifier.set("sources")
        from(sourceSets.main.get().allSource)
    }
    processResources {
        filteringCharset = Charsets.UTF_8.name()
    }

    jar {
        manifest {
            attributes(
                "paperweight-mappings-namespace" to "mojang"
            )
        }
    }

    bukkit {
        main = "de.erethon.aether.Aether"
        apiVersion = "1.21"
        authors = listOf("Malfrador")
        depend = listOf("QuestsXL")
        softDepend = listOf("Hephaestus", "Daedalus", "Factions")
    }

    assemble {
        dependsOn(reobfJar)
        dependsOn(sourcesJar)
    }
}


tasks.register<Copy>("deployToSharedServer") {
    doNotTrackState("")
    group = "Erethon"
    description = "Used for deploying the plugin to the shared server. runServer will do this automatically." +
            "This task is only for manual deployment when running runServer from another plugin."
    dependsOn(":jar")
    from(layout.buildDirectory.file("libs/Aether-$version.jar"))
    into("C:\\Dev\\Erethon\\plugins")
}

publishing {
    repositories {
        maven {
            name = "erethon"
            url = uri("https://repo.erethon.de/snapshots/")
            credentials(PasswordCredentials::class)
            authentication {
                create<BasicAuthentication>("basic")
            }
        }
    }
    publications {
        create<MavenPublication>("maven") {
            groupId = "${project.group}"
            artifactId = "Aether"
            version = "${project.version}"

            from(components["java"])
            artifact(tasks["sourcesJar"])
        }
    }
}


