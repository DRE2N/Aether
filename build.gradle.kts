import net.minecrell.pluginyml.bukkit.BukkitPluginDescription

repositories {
    mavenLocal()
    maven("https://repo.erethon.de/snapshots/")
    maven("https://repo.unnamed.team/repository/unnamed-public/")
    maven("https://repo.dmulloy2.net/repository/public/")
    maven("https://oss.sonatype.org/content/groups/public/")
    maven("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://ci.emc.gs/nexus/content/groups/aikar/")
    maven("https://repo.aikar.co/content/groups/aikar")
    maven("https://repo.md-5.net/content/repositories/releases/")
    maven("https://hub.spigotmc.org/nexus/content/groups/public/")
    maven("https://jitpack.io")
}

plugins {
    `java-library`
    `maven-publish`
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("xyz.jpenilla.run-paper") version "1.0.6" // Adds runServer and runMojangMappedServer tasks for testing
    id("io.github.goooler.shadow") version "8.1.5"
    id("net.minecrell.plugin-yml.bukkit") version "0.5.1"
}

group = "de.erethon.aether"
version = "1.0.1-SNAPSHOT"
description = "Mob and NPC plugin for Erethon"

java {
    // Configure the java toolchain. This allows gradle to auto-provision JDK 17 on systems that only have JDK 8 installed for example.
    toolchain.languageVersion.set(JavaLanguageVersion.of(21))
}

val papyrusVersion = "1.21.4-R0.1-SNAPSHOT"
paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.MOJANG_PRODUCTION

dependencies {
    paperweightDevelopmentBundle("de.erethon.papyrus", "dev-bundle", papyrusVersion) { isChanging = true }
    implementation("de.erethon:bedrock:1.4.0") { isTransitive = false }

    implementation("net.worldseed.multipart", "WorldSeedEntityEngine", "12.1")
    implementation("org.zeroturnaround:zt-zip:1.8")

    compileOnly("de.erethon.hephaestus:Hephaestus:1.0-SNAPSHOT")

}

tasks {
    // Configure reobfJar to run when invoking the build task

    runServer {
        if (!project.buildDir.exists()) {
            project.buildDir.mkdir()
        }
        val f = File(project.buildDir, "server.jar");
        uri("https://github.com/DRE2N/Papyrus/releases/download/latest/papyrus-paperclip-$papyrusVersion-mojmap.jar").toURL().openStream().use { it.copyTo(f.outputStream()) }
        serverJar(f)
        workingDir = File (project.buildDir, "test")
    }

    compileJava {
        options.encoding = Charsets.UTF_8.name()
        options.release.set(21)
    }

    javadoc {
        options.encoding = Charsets.UTF_8.name()
    }
    val javadocJar by creating(Jar::class) {
        dependsOn(javadoc)
        archiveClassifier.set("javadoc")
        from(javadoc)
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

    shadowJar {
        dependencies {
            include(dependency("de.erethon:bedrock:.*"))
            include(dependency("net.worldseed.multipart:WorldSeedEntityEngine:12.1"))
            // Make sure those match the WorldSeedEntityEngine versions
            include(dependency("org.zeroturnaround:zt-zip:1.8"))
            include(dependency("javax.json:javax.json-api:1.1.4"))
            include(dependency("org.glassfish:javax.json:1.1.4"))
            include(dependency("dev.hollowcube:mql:1.0.1"))
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

    assemble {
        dependsOn(reobfJar)
        dependsOn(shadowJar)
        dependsOn(javadocJar)
        dependsOn(sourcesJar)
    }
}

publishing {
    repositories {
        maven {
            name = "erethon"
            url = uri("https://reposilite.fyreum.de/releases/")
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
            artifact(tasks["javadocJar"])
            artifact(tasks["sourcesJar"])
        }
    }
}


