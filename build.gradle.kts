buildscript { repositories { mavenCentral() } }

plugins {
    id("dagpenger.rapid-and-rivers")
}

application {
    mainClass.set("no.nav.dagpenger.quiz.birgitte.AppKt")
}
