
plugins {
    java
    id("com.github.johnrengelman.shadow") version "8.1.1"
}

group = "com.zeenea"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Apache POI for Excel handling
    implementation("org.apache.poi:poi-ooxml:5.2.3")

    // Jackson for JSON
    implementation("com.fasterxml.jackson.core:jackson-databind:2.15.2")
    implementation("com.fasterxml.jackson.core:jackson-core:2.15.2")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310:2.15.2")

    // SLF4J logging
    implementation("org.slf4j:slf4j-api:2.0.7")
    implementation("org.slf4j:slf4j-simple:2.0.7")

    // Log4j logging
    implementation("org.apache.logging.log4j:log4j-api:2.22.0")
    implementation("org.apache.logging.log4j:log4j-core:2.22.0")

    // JUnit 5
    testImplementation("org.junit.jupiter:junit-jupiter:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("org.junit.jupiter:junit-jupiter-params:5.10.0")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher:1.10.0")
}


tasks.test {
    useJUnitPlatform()
}

tasks.named<com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar>("shadowJar") {
    archiveBaseName.set("excel-connector")
    archiveClassifier.set("")
    archiveVersion.set("1.0.0")
    mergeServiceFiles()
    manifest {
        attributes(mapOf("Main-Class" to "com.zeenea.connector.excel.demo.LocalRunner"))
    }
}


tasks.register("runLocal", JavaExec::class) {
    group = "application"
    mainClass.set("com.zeenea.connector.excel.demo.LocalRunner")
    classpath = sourceSets.main.get().runtimeClasspath
    args = listOf("./test-data")
}
