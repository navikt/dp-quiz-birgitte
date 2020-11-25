package no.nav.dagpenger.quiz.birgitte

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(System.getenv()).apply {
        Birgitte(this)
    }.start()
}
