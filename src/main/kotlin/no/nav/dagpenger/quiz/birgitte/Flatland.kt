package no.nav.dagpenger.quiz.birgitte

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.isMissingOrNull
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.concurrent.fixedRateTimer

internal class Delay(private val initalDelay: Long, private val period: Long, private val timeUnit: TimeUnit) {
    fun initalDelay() = timeUnit.toMillis(initalDelay)

    fun period() = timeUnit.toMillis(period)
}

internal class Flatland(rapidsConnection: RapidsConnection, delay: Delay = Delay(1, 15, TimeUnit.MINUTES)) :
    River.PacketListener {
    private companion object {
        val log = KotlinLogging.logger { }
        val sikkerLogg = KotlinLogging.logger("tjenestekall")
        val objectMapper =
            jacksonObjectMapper()
                .registerModule(JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
                .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
    }

    private val behovUtenLøsning = mutableMapOf<String, Pair<MessageContext, JsonMessage>>()

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "faktum_svar") }
            validate { it.forbid("@final") }
            validate { it.requireKey("@behovId", "@behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.interestedIn("@løsning", "søknad_uuid") }
        }.register(this)

        fixedRateTimer(initialDelay = delay.initalDelay(), period = delay.period()) {
            val komplette =
                behovUtenLøsning.iterator().asSequence().toList().filter { it.value.second.erKomplett() }.map { it.key }
            komplette.forEach {
                behovUtenLøsning.remove(it)
            }
            behovUtenLøsning
                .filterValues { (_, packet) ->
                    packet["@opprettet"].asLocalDateTime().isBefore(LocalDateTime.now().minusMinutes(30))
                }
                .forEach { (key, _) ->
                    behovUtenLøsning.remove(key).also {
                        if (it != null) {
                            sendUfullstendigBehovEvent(it)
                        }
                    }
                }
        }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val id = packet["@behovId"].asText()
        when (behovUtenLøsning[id]) {
            null -> behovUtenLøsning[id] = (context to packet)
            else -> behovUtenLøsning[id]?.also { it.second.kombinerLøsninger(packet) }
        }
    }

    private fun sendUfullstendigBehovEvent(pair: Pair<MessageContext, JsonMessage>) {
        val (context, behov) = pair
        val forventninger = behov.forventninger()
        val løsninger = behov.løsninger()
        val mangler = forventninger.minus(løsninger)
        loggUfullstendingBehov(behov, mangler)
        val behovId = behov["@behovId"].asText()
        val message =
            JsonMessage.newMessage(
                "behov_uten_fullstendig_løsning",
                mapOf(
                    "@id" to UUID.randomUUID(),
                    "@opprettet" to LocalDateTime.now(),
                    "behov_id" to behovId,
                    "behov_opprettet" to behov["@opprettet"].asLocalDateTime(),
                    "forventet" to forventninger,
                    "løsninger" to løsninger,
                    "mangler" to mangler,
                    "kontekst_id" to behov.søknadUUID(),
                    "ufullstendig_behov" to behov.toJson(),
                ),
            )

        context.publish(
            behovId,
            message.toJson(),
        )
    }

    private fun JsonMessage.erKomplett(): Boolean = this.forventninger().all { it in this.løsninger() }

    private fun JsonMessage.forventninger(): List<String> = this["@behov"].map(JsonNode::asText)

    private fun JsonMessage.løsninger(): List<String> = this["@løsning"].fieldNames().asSequence().toList()

    private fun JsonMessage.kombinerLøsninger(packet: JsonMessage) {
        if (this["@løsning"].isMissingOrNull()) {
            this["@løsning"] = objectMapper.createObjectNode()
        }

        val løsning = this["@løsning"] as ObjectNode

        packet["@løsning"].fields().forEach { (behovtype, delløsning) ->
            løsning.set<JsonNode>(behovtype, delløsning)
        }

        loggKombinering(this)
    }

    private fun loggKombinering(packet: JsonMessage) {
        withLoggingContext(
            mapOf(
                "behovId" to packet["@behovId"].asText(),
                "søknadId" to packet.søknadUUID(),
            ),
        ) {
            val løsninger = packet["@løsning"].fieldNames().asSequence().toSet()
            val behov = packet["@behov"].map(JsonNode::asText)
            val mangler = behov.minus(løsninger)
            val melding = "Har løsninger for [${løsninger.joinToString(", \n\t", "\n\t", "\n")}]. "

            if (mangler.isEmpty()) {
                log.info { "Ferdig! $melding" }
            } else {
                log.info { melding + "Venter på løsninger for [${mangler.joinToString(", \n\t", "\n\t", "\n")}]" }
            }
        }
    }

    private fun loggUfullstendingBehov(
        packet: JsonMessage,
        mangler: List<String>,
    ) {
        withLoggingContext(
            mapOf(
                "behovId" to packet["@behovId"].asText(),
                "søknadId" to packet.søknadUUID(),
            ),
        ) {
            listOf(log, sikkerLogg).forEach { logger ->
                logger.error {
                    "Mottok aldri løsning for ${mangler.joinToString { it }} innen 30 minutter."
                }
            }
        }
    }
}

private fun JsonMessage.søknadUUID(): String = this["søknad_uuid"].textValue() ?: "mangler"
