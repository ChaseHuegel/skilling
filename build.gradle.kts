plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta10"
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.incendo.org/repository/maven-releases/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")

    // Database
    implementation("com.zaxxer:HikariCP:6.3.0")
    implementation("org.xerial:sqlite-jdbc:3.49.1.0")

    // Command Framework (Incendo Cloud)
    implementation("org.incendo:cloud-paper:2.0.0")
    implementation("org.incendo:cloud-minecraft-extras:2.0.0")
    implementation("org.incendo:cloud-annotations:2.0.0")
    annotationProcessor("org.incendo:cloud-annotations:2.0.0")

    // Web GUI (Javalin 7)
    implementation("io.javalin:javalin:7.0.1") {
        exclude("org.slf4j")
        exclude("com.fasterxml.jackson.core")
    }
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:jul-to-slf4j:2.0.17")

    // Testing
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Frontend build integration will be added in Phase 4 when the Vue SPA is created.
tasks.named<Copy>("processResources") {
    val frontendDist = file("web/frontend/dist")
    if (frontendDist.exists()) {
        from("web/frontend/dist") {
            into("web/frontend")
        }
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
}

tasks {
    shadowJar {
        relocate("com.zaxxer.hikari", "io.github.chasehuegel.skilling.libs.hikari")
        relocate("org.sqlite", "io.github.chasehuegel.skilling.libs.sqlite")
        relocate("org.incendo.cloud", "io.github.chasehuegel.skilling.libs.cloud")
        relocate("io.javalin", "io.github.chasehuegel.skilling.libs.javalin")
        relocate("org.eclipse.jetty", "io.github.chasehuegel.skilling.libs.jetty")
        minimize()
    }

    runServer {
        minecraftVersion("1.21.8")
        jvmArgs("-Xms2G", "-Xmx2G")
    }

    processResources {
        val props = mapOf("version" to version)
        filesMatching("paper-plugin.yml") {
            expand(props)
        }
    }

    assemble {
        dependsOn(shadowJar)
    }
}
