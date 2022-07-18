plugins {
    kotlin("jvm") version "1.6.21"
    id("org.flywaydb.flyway") version "9.0.1"
}

group = "fr.corpauration"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

buildscript {
    dependencies {
        classpath("org.postgresql:postgresql:42.3.6")
    }
}

configurations {
//    flywayMigration
}

dependencies {
    implementation("org.flywaydb:flyway-core:9.0.1")
//    flywayMigration(project(":"))
}

flyway {
    url = "jdbc:postgresql://localhost:5432/cyrelV2"
    user = "cyrel"
//    locations = arrayOf("filesystem:/home/alyrow/IdeaProjects/v2/cyrel-backend/src/main/resources/db/migration/")
}