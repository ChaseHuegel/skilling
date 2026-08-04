plugins {
    id("java-library")
    id("xyz.jpenilla.run-paper") version "3.0.2"
    id("com.gradleup.shadow") version "9.0.0-beta10"
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.extendedclip.com/releases/")
    maven("https://repo.incendo.org/repository/maven-releases/")

}

dependencies {
    implementation(project(":skilling-api"))

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
    // Jackson is provided by the Paper runtime; compileOnly so the plugin
    // never shades it (Javalin's default JSON mapper is Jackson).
    compileOnly("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    implementation("org.slf4j:slf4j-api:2.0.17")
    implementation("org.slf4j:jul-to-slf4j:2.0.17")

    // External integrations
    implementation("org.bstats:bstats-bukkit:3.1.0")
    // PlaceholderAPI is provided by the server plugin; compileOnly so the hook
    // can extend PlaceholderExpansion without shading PlaceholderAPI itself.
    compileOnly("me.clip:placeholderapi:2.12.3")

    // Testing
    testImplementation("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    testImplementation("org.junit.jupiter:junit-jupiter:5.12.1")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("org.mockito:mockito-core:5.15.2")
    testImplementation("org.mockito:mockito-junit-jupiter:5.15.2")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:2.13.4.2")
    // PlaceholderAPI on the test classpath so the hook's nested expansion class
    // resolves even though no test ever instantiates it.
    testImplementation("me.clip:placeholderapi:2.12.3")
}

val shouldBuildFrontend = providers.provider {
    file("web/frontend/package.json").exists() && file("web/frontend/node_modules").exists()
}

val buildFrontend = tasks.register("buildFrontend", Exec::class.java) {
    description = "Build the Vue frontend for production"
    workingDir = file("web/frontend")
    commandLine("npm", "run", "build")
    outputs.dir("web/frontend/dist")
    onlyIf { shouldBuildFrontend.get() }
}

tasks.named<Copy>("processResources") {
    dependsOn(buildFrontend)
    val frontendDist = file("web/frontend/dist")
    if (frontendDist.exists()) {
        from("web/frontend/dist") {
            into("web/frontend")
        }
    }
}

tasks.named("shadowJar") {
    dependsOn(buildFrontend)
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
        relocate("org.bstats", "io.github.chasehuegel.skilling.libs.bstats")
        minimize()
    }

    runServer {
        dependsOn(buildFrontend)
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

publishing {
    publications {
        create<MavenPublication>("skilling") {
            from(components["java"])
            artifactId = "skilling"
            version = project.version as String

            pom {
                name = "Skilling"
                description = "A flexible, data-driven RPG skills engine for PaperMC"
                url = "https://github.com/chasehuegel/skilling"
                licenses {
                    license {
                        name = "Apache-2.0"
                        url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                    }
                }
                developers {
                    developer {
                        id = "chasehuegel"
                        name = "ChaseHuegel"
                        email = "chase@bitfish.dev"
                    }
                }
                scm {
                    connection = "scm:git:git://github.com/chasehuegel/skilling.git"
                    developerConnection = "scm:git:ssh://github.com/chasehuegel/skilling.git"
                    url = "https://github.com/chasehuegel/skilling"
                }
            }
        }
    }

    repositories {
        maven {
            name = "Local"
            url = uri(layout.buildDirectory.dir("repo"))
        }
    }
}
