pluginManagement {
    repositories {
        gradlePluginPortal()
        maven("https://papermc.io/repo/repository/maven-public/")
    }
}

rootProject.name = "Aether"

val questsXLDir = file("../QuestsXL")
if (questsXLDir.exists()) {
    includeBuild(questsXLDir) {
        dependencySubstitution {
            substitute(module("de.erethon.questsxl:QuestsXL")).using(project(":plugin"))
        }
    }
}
