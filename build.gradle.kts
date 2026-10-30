plugins {
    `java-library`
}

group = "org.jsoup"
version = "1.23.2"
description = "jsoup Java HTML Parser"

repositories {
    mavenCentral()
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.1")
    compileOnly("com.google.re2j:re2j:1.8")

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation(platform("io.netty:netty-bom:4.2.17.Final"))
    testImplementation("io.netty:netty-codec-http")
    testImplementation("io.netty:netty-handler")
    testImplementation("org.jspecify:jspecify:1.0.1")
    testImplementation("com.google.re2j:re2j:1.8")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
    options.release = 21
    options.compilerArgs.add("-Xlint:none")
}

tasks.compileJava {
    options.compilerArgs.add("-Xpkginfo:always")
    // re2j is an optional, non-modular jar. Keep its types visible while compiling this module.
    options.compilerArgs.addAll(listOf("--add-reads", "org.jsoup=ALL-UNNAMED"))
}

tasks.processResources {
    from("LICENSE") {
        into("META-INF/jsoup")
    }
}

tasks.jar {
    exclude("org/jsoup/examples/**")
    manifest {
        attributes(
            "Implementation-Title" to project.description,
            "Implementation-Version" to project.version,
            "Main-Class" to "org.jsoup.cli.Main",
        )
    }
}

val integrationTestPatterns = listOf("**/IT*.class", "**/*IT.class", "**/*ITCase.class")

tasks.test {
    useJUnitPlatform()
    // Smaller stack to find stack overflows.
    jvmArgs("-Xss640k")
    exclude(integrationTestPatterns)
}

val integrationTest by tasks.registering(Test::class) {
    description = "Runs the integration tests, excluding long-running ones."
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        excludeTags("long-running")
    }
    include(integrationTestPatterns)
    systemProperties(
        "junit.jupiter.execution.parallel.enabled" to "true",
        "junit.jupiter.execution.parallel.mode.default" to "same_thread",
        "junit.jupiter.execution.parallel.mode.classes.default" to "same_thread",
        "junit.jupiter.execution.parallel.config.strategy" to "fixed",
        "junit.jupiter.execution.parallel.config.fixed.parallelism" to "8",
    )
    shouldRunAfter(tasks.test)
}

tasks.check {
    dependsOn(integrationTest)
}
