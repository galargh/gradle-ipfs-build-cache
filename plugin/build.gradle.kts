plugins {
    // Apply the Java Gradle plugin development plugin to add support for developing Gradle plugins
    `java-gradle-plugin`
    `maven-publish`

    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
}

group = "caching.ipfs"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()

    // Use Cloudsmith and jcenter to jvm-libp2p.
    maven("https://dl.cloudsmith.io/public/libp2p/jvm-libp2p/maven/")

    jcenter()
}

publishing {
    repositories {
        maven {
            url = uri(layout.buildDirectory.dir(".m2/repository"))
        }
    }
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")

    // Use jvm-libp2p.
    implementation("io.libp2p:jvm-libp2p-minimal:0.8.3-RELEASE")

    // Use log4j.
    implementation("org.apache.logging.log4j:log4j-api:2.14.1")
    implementation("org.apache.logging.log4j:log4j-core:2.14.1")

    // Use common-lang for SerializationUtils.
    implementation("org.apache.commons:commons-lang3:3.12.0")

    // Use the Kotlin test library.
    testImplementation("org.jetbrains.kotlin:kotlin-test")

    // Use the Kotlin JUnit integration.
    testImplementation("org.jetbrains.kotlin:kotlin-test-junit")

    // Use the Kotlin Mockito library.
    testImplementation("org.mockito.kotlin:mockito-kotlin:4.0.0")
}

gradlePlugin {
    // Define the plugin
    val ipfsBuildCache by plugins.creating {
        id = "caching.ipfs.ipfs-build-cache"
        implementationClass = "caching.ipfs.IpfsBuildCachePlugin"
    }
}

// Add a source set for the functional test suite
val functionalTestSourceSet = sourceSets.create("functionalTest") {
}

configurations["functionalTestImplementation"].extendsFrom(configurations["testImplementation"])

// Add a task to run the functional tests
val functionalTest by tasks.registering(Test::class) {
    testClassesDirs = functionalTestSourceSet.output.classesDirs
    classpath = functionalTestSourceSet.runtimeClasspath
}

gradlePlugin.testSourceSets(functionalTestSourceSet)

tasks.named<Task>("check") {
    // Run the functional tests as part of `check`
    dependsOn(functionalTest)
}

val copyDependencies by tasks.registering(Copy::class) {
    from(configurations.runtimeClasspath)
    into("${project.buildDir}/libs/dependencies")
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
}

val cli by tasks.registering(DefaultTask::class) {
    dependsOn(copyDependencies)
    dependsOn(tasks.jar)
    doLast {
        logger.lifecycle("java -cp \"${copyDependencies.get().destinationDir}/*:${tasks.jar.get().destinationDirectory.get()}/*\" caching.ipfs.internal.IpfsBuildCacheCliKt")
    }
}
