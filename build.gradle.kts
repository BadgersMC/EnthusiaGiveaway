import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.1.0"
    id("com.gradleup.shadow") version "8.3.5"
}

group = "net.badgersmc.giveaway"
version = "0.1.0"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
    maven("https://jitpack.io")

    // Nexus releases — GitHub Packages. Needs gpr.user + gpr.token in
    // ~/.gradle/gradle.properties (or GITHUB_ACTOR + GITHUB_TOKEN env on CI).
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/BadgersMC/Nexus")
        credentials {
            username = providers.gradleProperty("gpr.user").orNull
                ?: System.getenv("GITHUB_ACTOR")
            password = providers.gradleProperty("gpr.token").orNull
                ?: System.getenv("GITHUB_TOKEN")
        }
    }

    // Opt-in to a locally-published Nexus snapshot: ./gradlew -PuseMavenLocal=true …
    if (providers.gradleProperty("useMavenLocal").orNull == "true") {
        mavenLocal()
    }
}

dependencies {
    // Platform
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")

    // Nexus DI + coroutines bridge (shaded)
    implementation("net.badgersmc:nexus-core:1.11.0")
    implementation("net.badgersmc:nexus-paper:1.11.0")
    implementation("net.badgersmc:nexus-persistence:1.11.0")
    implementation("net.badgersmc:nexus-scheduler:1.11.0")
    implementation("net.badgersmc:nexus-paper-loader:1.11.0")

    // Kotlin + coroutines (downloaded at runtime by PaperLoader)
    compileOnly(kotlin("stdlib"))
    compileOnly("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.8.0")

    // Storage (downloaded at runtime by PaperLoader)
    compileOnly("org.xerial:sqlite-jdbc:3.45.1.0")
    compileOnly("com.zaxxer:HikariCP:5.1.0")
    compileOnly("org.jetbrains.exposed:exposed-core:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-dao:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    compileOnly("org.jetbrains.exposed:exposed-java-time:0.55.0")

    // GUI: InventoryFramework (shaded — small, no loader hassle)
    implementation("com.github.stefvanschie.inventoryframework:IF:0.11.6")

    // Adventure (bundled with Paper)
    compileOnly("net.kyori:adventure-api:4.17.0")
    compileOnly("net.kyori:adventure-text-minimessage:4.17.0")

    // PlaceholderAPI (optional hook)
    compileOnly("me.clip:placeholderapi:2.11.6")

    // Testing
    testImplementation(kotlin("test"))
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("io.mockk:mockk:1.13.10")
    testImplementation("com.github.seeseemelk:MockBukkit-v1.21:3.127.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.8.0")
    testImplementation("org.xerial:sqlite-jdbc:3.45.1.0")
    testImplementation("com.zaxxer:HikariCP:5.1.0")
    testImplementation("org.jetbrains.exposed:exposed-core:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-dao:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-jdbc:0.55.0")
    testImplementation("org.jetbrains.exposed:exposed-java-time:0.55.0")

    // Konsist for SPEAR layer-rule enforcement
    testImplementation("com.lemonappdev:konsist:0.17.3")
}

kotlin {
    jvmToolchain(21)
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("")
    mergeServiceFiles()
    exclude("kotlin/**")
    exclude("kotlinx/coroutines/**")
    exclude("META-INF/kotlin*")
    exclude("_COROUTINE/**")
}

tasks.processResources {
    filesMatching("paper-plugin.yml") {
        expand("version" to project.version)
    }
}

tasks.build {
    dependsOn(tasks.shadowJar)
}
