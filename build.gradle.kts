plugins {
    `java-library`
}

group = "org.jsoup"
version = "1.23.2"
description = "jsoup Java HTML Parser"

repositories {
    mavenCentral()
}

// Multi-release overlay: compiled for Java 11 and packaged under META-INF/versions/11.
// The Java 8 base loads these classes reflectively when they are available.
val java11: SourceSet by sourceSets.creating {
    java.setSrcDirs(listOf("src/main/java11"))
    java.destinationDirectory = layout.buildDirectory.dir("classes/java11/META-INF/versions/11")
    resources.setSrcDirs(emptyList<String>())
}

dependencies {
    compileOnly("org.jspecify:jspecify:1.0.1")
    compileOnly("com.google.re2j:re2j:1.8")

    "java11CompileOnly"("org.jspecify:jspecify:1.0.1")
    "java11CompileOnly"(sourceSets.main.get().output)

    testImplementation(platform("org.junit:junit-bom:5.14.4"))
    testImplementation("org.junit.jupiter:junit-jupiter")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
    testImplementation("com.google.code.gson:gson:2.14.0")
    testImplementation(platform("io.netty:netty-bom:4.2.17.Final"))
    testImplementation("io.netty:netty-codec-http")
    testImplementation("io.netty:netty-handler")
    testImplementation("org.jspecify:jspecify:1.0.1")
    testImplementation("com.google.re2j:re2j:1.8")
    testImplementation(java11.output)
}

sourceSets.test {
    java.srcDir("src/test/java11")
}

tasks.withType<JavaCompile>().configureEach {
    options.encoding = "UTF-8"
}

tasks.compileJava {
    options.release = 8
    options.compilerArgs.addAll(listOf("-Xpkginfo:always", "-Xlint:none"))
}

tasks.named<JavaCompile>("compileJava11Java") {
    options.release = 11
    val baseClasses = sourceSets.main.get().output.classesDirs
    options.compilerArgumentProviders.add(CommandLineArgumentProvider {
        listOf("--patch-module", "org.jsoup=${baseClasses.asPath}")
    })
}

tasks.compileTestJava {
    options.release = 11
    options.compilerArgs.add("-Xlint:none")
}

tasks.processResources {
    from("LICENSE") {
        into("META-INF/jsoup")
    }
}

tasks.jar {
    exclude("org/jsoup/examples/**")
    into("META-INF/versions/11") {
        from(java11.output)
    }
    manifest {
        attributes(
            "Multi-Release" to "true",
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
