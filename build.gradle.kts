buildscript { repositories { mavenCentral() } }

plugins {
    id("common")
    application
}

dependencies {
    implementation(libs.rapids.and.rivers)
    implementation(libs.konfig)
    implementation(libs.kotlin.logging)
}

application {
    mainClass.set("no.nav.dagpenger.quiz.birgitte.AppKt")
}
