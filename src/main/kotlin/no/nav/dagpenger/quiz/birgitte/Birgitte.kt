package no.nav.dagpenger.quiz.birgitte

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

class Birgitte(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.forbid("@final") }
            validate { it.requireKey("@id", "@behov", "@løsning", "fakta") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("søknad_uuid") }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerLogg = KotlinLogging.logger("tjenestekall")
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        loggBehov(packet)
        packet["@løsning"].fields().forEach { (behov, løsning) ->
            val faktum = packet["fakta"].find { faktum -> faktum["behov"].asText() == behov } as ObjectNode
            faktum.set<JsonNode>("svar", løsning)
        }
        packet["@event_name"] = "faktum_svar"
        packet["@final"] = true
        context.send(packet.toJson())
    }

    private fun loggBehov(packet: JsonMessage) {
        withLoggingContext(
            "behovId" to packet["@id"].asText(),
            "søknad_uuid" to packet["søknad_uuid"].asText()
        ) {
            listOf(log, sikkerLogg).forEach { logger ->
                logger.info {
                    val løsninger = packet["@løsning"].fieldNames().asSequence().joinToString(", ")
                    "Mottok løsning for $løsninger"
                }
            }
        }
    }
}
