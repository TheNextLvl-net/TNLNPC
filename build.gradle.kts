import io.papermc.hangarpublishplugin.model.Platforms
import net.minecrell.pluginyml.paper.PaperPluginDescription

plugins {
    id("java")
    id("com.gradleup.shadow") version "9.0.0-beta9"
    id("io.papermc.hangar-publish-plugin") version "0.1.2"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.14"
    id("net.minecrell.plugin-yml.paper") version "0.6.0"
}

group = "net.thenextlvl.characters"
version = "0.1.0"

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks.compileJava {
    options.release.set(21)
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.thenextlvl.net/releases")
    maven("https://repo.inventivetalent.org/repository/public/")
}

dependencies {
    paperweight.paperDevBundle("1.21.4-R0.1-SNAPSHOT")

    implementation("net.thenextlvl.core:i18n:1.0.20")
    implementation("net.thenextlvl.core:paper:2.0.3")
    implementation("org.bstats:bstats-bukkit:3.1.0")
    implementation("org.mineskin:java-client-java11:3.0.3-SNAPSHOT") {
        exclude("com.google.code.gson", "gson")
        exclude("com.google.guava", "guava")
    }
    implementation(project(":api"))

    //testImplementation("io.papermc.paper:paper-api:1.21.4-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.12.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
}

tasks.test {
    useJUnitPlatform()
}

tasks.shadowJar {
    minimize()
    relocate("org.bstats", "net.thenextlvl.character.bstats")
    relocate("org.mineskin", "net.thenextlvl.character.mineskin")
}

paper {
    name = "Characters"
    main = "net.thenextlvl.character.plugin.CharacterPlugin"
    apiVersion = "1.21"
    website = "https://thenextlvl.net"
    authors = listOf("NonSwag")
    foliaSupported = true

    serverDependencies {
        register("HologramAPI") {
            load = PaperPluginDescription.RelativeLoadOrder.BEFORE
            required = false
        }
    }
}

val versionString: String = project.version as String
val isRelease: Boolean = !versionString.contains("-pre")

val versions: List<String> = (property("gameVersions") as String)
    .split(",")
    .map { it.trim() }

hangarPublish { // docs - https://docs.papermc.io/misc/hangar-publishing
    publications.register("plugin") {
        id.set("Characters")
        version.set(versionString)
        channel.set(if (isRelease) "Release" else "Snapshot")
        apiKey.set(System.getenv("HANGAR_API_TOKEN"))
        platforms.register(Platforms.PAPER) {
            jar.set(tasks.shadowJar.flatMap { it.archiveFile })
            platformVersions.set(versions)
        }
    }
}