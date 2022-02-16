package no.nav.dagpenger.quiz.birgitte

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.concurrent.TimeUnit

internal class FlatlandTest {

    private val delay = Delay(1, 1, TimeUnit.MILLISECONDS)
    @Test
    fun `skal varsle om uløste behov`() = runBlocking {
        val rapid: TestRapid = TestRapid().apply {
            Flatland(rapidsConnection = this, delay = delay)
        }
        rapid.sendTestMessage(uLøsteBehovJson(opprettet = LocalDateTime.now().minusHours(1))) // Manipulate time to trigger 30 minute threshold
        delay(100)
        with(rapid.inspektør) {
            assertEquals(1, size)
            assertEquals("behov_uten_fullstendig_løsning", field(0, "@event_name").asText())
            assertNotNull(field(0, "@opprettet").asLocalDateTime())
            assertNotNull(field(0, "behov_id").asText())
            assertNotNull(field(0, "søknad_uuid").asText())
            assertTrue(field(0, "forventet").asIterable().toList().isNotEmpty())
            assertTrue(field(0, "mangler").asIterable().toList().isNotEmpty())
            assertNotNull(field(0, "behov_opprettet").asLocalDateTime())
            assertNotNull(field(0, "ufullstendig_behov"))
        }
    }

    @Test
    fun `skal ikke varsle om løste behov`() = runBlocking {
        val rapid: TestRapid = TestRapid().apply {
            Flatland(rapidsConnection = this, delay = delay)
        }
        rapid.sendTestMessage(løstBehovJson)
        rapid.sendTestMessage(uLøsteBehovJson(opprettet = LocalDateTime.now().minusHours(1))) // Manipulate time to trigger 30 minute threshold
        delay(1000)
        with(rapid.inspektør) {
            assertEquals(1, size)
        }
    }

    @Test
    fun `Ingen løste behov`() = runBlocking {
        val rapid: TestRapid = TestRapid().apply {
            Flatland(rapidsConnection = this, delay = delay)
        }
        rapid.sendTestMessage(ingenLøsteBehov(opprettet = LocalDateTime.now().minusHours(1))) // Manipulate time to trigger 30 minute threshold
        delay(1000)
        with(rapid.inspektør) {
            assertEquals(1, size)
        }
    }
}
@Language("json")
private val løstBehovJson =
    """
{
  "@event_name": "faktum_svar",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "5567482C-2175-46DE-9862-8B98B302C7F6",
  "fnr": "123",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "fakta": [ 
    {
      "id": "666",
      "behov": "Registreringsdato",
      "type": "localdate"
    },
    {
      "id": "777",
      "behov": "Fødselsdato",
      "type": "localdate"
    }
  ],
  "@behov": [
    "Registreringsdato",
    "Fødselsdato"
  ],
  "@løsning": {
    "Registreringsdato": "2020-11-01",
    "Fødselsdato": "1998-11-01"
  },
  "Søknadstidspunkt": "2020-11-09"
}
    """.trimIndent()

@Language("json")
private fun uLøsteBehovJson(opprettet: LocalDateTime) =
    """
{
  "@event_name": "faktum_svar",
  "@opprettet": "$opprettet",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "fnr": "123",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "fakta": [ 
    {
      "id": "666",
      "behov": "Registreringsdato",
      "type": "localdate"
    },
    {
      "id": "777",
      "behov": "Fødselsdato",
      "type": "localdate"
    }
  ],
  "@behov": [
    "Registreringsdato",
    "Fødselsdato"
  ],
  "@løsning": {
    "Registreringsdato": "2020-11-01"
  },
  "Søknadstidspunkt": "2020-11-09"
}
    """.trimIndent()

@Language("json")
private fun ingenLøsteBehov(opprettet: LocalDateTime) =
    """
{
  "@event_name": "faktum_svar",
  "@opprettet": "$opprettet",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "fnr": "123",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "fakta": [ 
    {
      "id": "666",
      "behov": "Registreringsdato",
      "type": "localdate"
    },
    {
      "id": "777",
      "behov": "Fødselsdato",
      "type": "localdate"
    }
  ],
  "@behov": [
    "Registreringsdato",
    "Fødselsdato"
  ],
  "Søknadstidspunkt": "2020-11-09"
}
    """.trimIndent()
