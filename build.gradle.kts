plugins {
    id("java")
}

group = "gay.realmromp"
version = "2.0.1"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
}

dependencies {
    @Suppress("VulnerableLibrariesLocal")
    compileOnly("io.papermc.paper:paper-api:1.19.2-R0.1-SNAPSHOT")
    implementation("com.github.stateless4j:stateless4j:2.6.0")
}

java {
    toolchain.languageVersion.set(JavaLanguageVersion.of(17))
}
