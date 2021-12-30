package no.nav.dagpenger.quiz.birgitte

import no.nav.helse.rapids_rivers.RapidApplication

fun main() {
    RapidApplication.create(Configuration.asMap()).apply {
        Birgitte(this)
        // Flatland(this)
    }.start()
}
