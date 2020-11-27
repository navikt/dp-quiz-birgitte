package no.nav.dagpenger.quiz.birgitte

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
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
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: RapidsConnection.MessageContext) {
        packet["@løsning"].fields().forEach { (behov, løsning) ->
            val faktum = packet["fakta"].find { faktum -> faktum["behov"].asText() == behov } as ObjectNode
            faktum.set<JsonNode>("svar", løsning)
        }
        packet["@event_name"] = "faktum_svar"
        packet["@final"] = true
        context.send(packet.toJson())
    }
}
