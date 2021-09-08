import org.gradle.api.tasks.testing.logging.TestExceptionFormat
import org.gradle.api.tasks.testing.logging.TestLogEvent
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    id("dagpenger.spotless")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(14))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation(platform(kotlin("bom")))
    implementation(kotlin("stdlib-jdk8"))

    testImplementation(Junit5.api)
    testRuntimeOnly(Junit5.engine)
}

tasks {
    test {
        useJUnitPlatform()
        testLogging {
            showExceptions = true
            showStackTraces = true
            exceptionFormat = TestExceptionFormat.FULL
            events = setOf(TestLogEvent.PASSED, TestLogEvent.SKIPPED, TestLogEvent.FAILED)
        }
    }

    withType<KotlinCompile>().all {
        kotlinOptions.jvmTarget = JavaVersion.VERSION_14.toString()
    }
}
