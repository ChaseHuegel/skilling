plugins {
    id("java-library")
    id("maven-publish")
}

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
}

java {
    toolchain.languageVersion = JavaLanguageVersion.of(21)
    withJavadocJar()
    withSourcesJar()
}

publishing {
    publications {
        create<MavenPublication>("skillingApi") {
            from(components["java"])
            artifactId = "skilling-api"
            version = project.version.toString()

            pom {
                name = "Skilling API"
                description = "API for the Skilling RPG skills engine for PaperMC"
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
            }
        }
    }
}
