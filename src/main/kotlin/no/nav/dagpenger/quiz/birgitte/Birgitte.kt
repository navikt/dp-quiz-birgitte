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
            validate { it.requireValue("@event_name", "faktum_svar") }
            validate { it.forbid("@final") }
            validate { it.requireKey("@behovId", "@behov", "@løsning", "fakta") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("søknad_uuid") }
        }.register(this)
    }

    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerLogg = KotlinLogging.logger("tjenestekall.Birgitte")
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        withLoggingContext(
            "behovId" to packet["@behovId"].asText(),
            "søknadId" to packet["søknad_uuid"].asText(),
        ) {
            val løsninger = packet["@løsning"].fieldNames().asSequence().joinToString(", ")
            logger.info("Mottok løsning for $løsninger")
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
                            logger.error { "Kunne ikke generere svar for generator faktum for $behov. (se tjenestekall for pakka)" }
                            sikkerLogg.error(
                                e,
                            ) { "Kunne ikke generere svar for generator faktum for $behov. Pakka ser sånn ut: ${packet.toJson()}" }
                        }
                    }

                    else -> faktum.set<JsonNode>("svar", løsning)
                }
            }
            packet["@final"] = true
            context.publish(packet.toJson())
        }
    }
}
