plugins {
    // Apply the Kotlin JVM plugin to add support for Kotlin.
    id("org.jetbrains.kotlin.jvm") version "1.5.31"
}

group = "example"
version = "0.0.1"

java {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
}

repositories {
    // Use Maven Central for resolving dependencies.
    mavenCentral()
}

dependencies {
    // Align versions of all Kotlin components
    implementation(platform("org.jetbrains.kotlin:kotlin-bom"))

    // Use the Kotlin JDK 8 standard library.
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
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
