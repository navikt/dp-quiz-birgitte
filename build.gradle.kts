plugins {
    application
    id(Shadow.shadow) version Shadow.version
    kotlin("jvm") version "1.4.20"
    id(Spotless.spotless) version Spotless.version
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

application {
    mainClassName = "no.nav.dagpenger.quiz.birgitte.AppKt"
}

dependencies {
    implementation(RapidAndRivers)
    implementation(Konfig.konfig)
    implementation(Kotlin.Logging.kotlinLogging)

    testImplementation(Mockk.mockk)
    testImplementation(Junit5.engine)
    testImplementation(Junit5.api)
}

spotless {
    kotlin {
        ktlint(Ktlint.version)
    }
    kotlinGradle {
        target("*.gradle.kts", "buildSrc/**/*.kt*")
        ktlint(Ktlint.version)
    }
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
