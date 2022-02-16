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
            validate { it.requireKey("@id", "@behov", "@løsning", "fakta") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("søknad_uuid") }
        }.register(this)
    }

    companion object {
        private val log = KotlinLogging.logger { }
        private val sikkerLogg = KotlinLogging.logger("tjenestekall")
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        loggBehov(packet)
        packet["@løsning"].fields().forEach { (behov, løsning) ->
            val faktum = packet["fakta"].find { faktum -> faktum["behov"].asText() == behov } as ObjectNode
            when (faktum["type"].asText()) {
                "generator" -> {
                    val svar = jacksonObjectMapper().createArrayNode()
                    løsning.forEach { enLøsning ->
                        faktum["templates"].deepCopy<ArrayNode>().also { templates ->
                            enLøsning.fields().forEach { (key, value) ->
                                val matchendeFaktum = templates.find { it["navn"].asText() == key } as ObjectNode
                                matchendeFaktum.set<JsonNode>("svar", value)
                            }
                        }.also {
                            svar.add(it)
                        }
                    }
                    faktum.set<JsonNode>("svar", svar)
                }
                else -> faktum.set<JsonNode>("svar", løsning)
            }
        }
        packet["@final"] = true
        context.publish(packet.toJson())
    }

    private fun loggBehov(packet: JsonMessage) {
        withLoggingContext(
            "behovId" to packet["@id"].asText(),
            "søknad_uuid" to packet["søknad_uuid"].asText()
        ) {
            listOf(log, sikkerLogg).forEach { logger ->
                logger.info {
                    val løsninger = packet["@løsning"].fieldNames().asSequence().joinToString(", ")
                    "Mottok løsning for $løsninger for søknad ${packet["søknad_uuid"].asText()}"
                }
            }
        }
    }
}
