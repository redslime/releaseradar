import org.jooq.meta.jaxb.Logging

val kordVersion = "0.9.0"
val spotifyVersion = "4.0.0"
val log4jVersion = "2.20.0"
val jooqVersion = "3.18.4"
val gsonVersion = "2.10.1"
val emoji4jVersion = "14.0.0"

plugins {
    kotlin("jvm") version "1.8.20"
    id("nu.studer.jooq") version "8.2"
}

group = "xyz.redslime"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

dependencies {
    testImplementation(kotlin("test"))
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.apache.logging.log4j:log4j-core:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-api:$log4jVersion")
    implementation("org.apache.logging.log4j:log4j-slf4j2-impl:$log4jVersion")
    implementation("dev.kord:kord-core:$kordVersion")
    implementation("com.adamratzman:spotify-api-kotlin-core:$spotifyVersion")
    implementation("org.jooq:jooq:$jooqVersion")
    implementation("com.mysql:mysql-connector-j:8.0.32")
    implementation("com.google.code.gson:gson:$gsonVersion")
    implementation("com.sigpwned:emoji4j-core:$emoji4jVersion")
    jooqGenerator("com.mysql:mysql-connector-j:8.0.32")
}

tasks.test {
    useJUnitPlatform()
}

jooq {
    configurations {
        create("main") {  // name of the jOOQ configuration
            jooqConfiguration.apply {
                logging = Logging.WARN
                jdbc.apply {
                    driver = System.getenv("dbDriver")
                    url = System.getenv("dbHost")
                    user = System.getenv("dbUser")
                    password = System.getenv("dbPassword")
                }
                generator.apply {
                    name = "org.jooq.codegen.KotlinGenerator"
                    database.apply {
                        name = "org.jooq.meta.mysql.MySQLDatabase"
                        excludes = "(?i:information_schema\\..*)"
                    }
                    generate.apply {
                        isJavaTimeTypes = true
                    }
                    target.apply {
                        packageName = "xyz.redslime.releaseradar.db"
                        directory = "src/generated/jooq"
                    }
                    strategy.name = "org.jooq.codegen.DefaultGeneratorStrategy"
                }
            }
        }
    }
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(17))
    }
}

tasks.named<nu.studer.gradle.jooq.JooqGenerate>("generateJooq") { allInputsDeclared.set(true) }

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    manifest {
        attributes["Main-Class"] = "xyz.redslime.releaseradar.MainKt"
    }
    archiveFileName.set("releaseradar.jar")
    val sourcesMain = sourceSets.main.get()
    val contents = configurations.runtimeClasspath.get()
        .map { if (it.isDirectory) it else zipTree(it) } +
            sourcesMain.output
    from(contents)
}