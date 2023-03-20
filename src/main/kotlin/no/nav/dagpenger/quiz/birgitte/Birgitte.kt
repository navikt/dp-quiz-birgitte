package no.nav.dagpenger.quiz.birgitte

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ArrayNode
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

class Birgitte(rapidsConnection: RapidsConnection) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.forbid("@final") }
            validate { it.requireKey("@behovId", "@behov", "@løsning", "fakta") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("søknad_uuid") }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerLogg = KotlinLogging.logger("tjenestekall")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        withLoggingContext(
            "behovId" to packet["@behovId"].asText(),
            "søknad_uuid" to packet["søknad_uuid"].asText(),
        ) {
            loggBehov(packet)
            packet["@løsning"].fields().forEach { (behov, løsning) ->
                val faktum = packet["fakta"].find { faktum -> faktum["behov"].asText() == behov } as ObjectNode
                when (faktum["type"].asText()) {
                    "generator" -> {
                        try {
                            val svar = jacksonObjectMapper().createArrayNode()
                            løsning.forEach { enLøsning ->
                                faktum["templates"].deepCopy<ArrayNode>().also { templates ->
                                    enLøsning.fields().forEach { (key, value) ->
                                        val matchendeFaktum =
                                            templates.find { it["navn"].asText() == key } as ObjectNode
                                        matchendeFaktum.set<JsonNode>("svar", value)
                                    }
                                }.also {
                                    svar.add(it)
                                }
                            }
                            faktum.set<JsonNode>("svar", svar)
                        } catch (e: NullPointerException) {
                            sikkerLogg.error(e) { "Kunne ikke generere svar for generator faktum. Pakka ser sånn ut: ${packet.toJson()}" }
                        }
                    }
                    else -> faktum.set<JsonNode>("svar", løsning)
                }
            }
            packet["@final"] = true
            context.publish(packet.toJson())
        }
    }

    private fun loggBehov(packet: JsonMessage) {
        listOf(log, sikkerLogg).forEach { logger ->
            logger.info {
                val løsninger = packet["@løsning"].fieldNames().asSequence().joinToString(", ")
                "Mottok løsning for $løsninger for søknad ${packet["søknad_uuid"].asText()}"
            }
        }
    }
}
