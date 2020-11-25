plugins {
    application
    id(Shadow.shadow) version Shadow.version
    kotlin("jvm") version "1.4.20"
}

repositories {
    jcenter()
    maven("https://jitpack.io")
}

application {
    mainClass.set("no.nav.dagpenger.quiz.birgitte.AppKt")
}

dependencies {
    implementation("com.github.navikt:rapids-and-rivers:1.9ea2f5e")

    implementation(Kotlin.Logging.kotlinLogging)

    testImplementation(Mockk.mockk)
    testImplementation(Junit5.engine)
    testImplementation(Junit5.api)
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}
