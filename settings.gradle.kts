rootProject.name = "dp-quiz-birgitte"

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
        maven(url = "https://dl.bintray.com/gradle/gradle-plugins")
    }
}
dependencyResolutionManagement {
    repositories {
        maven("https://github-package-registry-mirror.gc.nav.no/cached/maven-release")
    }
    versionCatalogs {
        create("libs") {
            from("no.nav.dagpenger:dp-version-catalog:20251030.227.c59fb6")
        }
    }
}
