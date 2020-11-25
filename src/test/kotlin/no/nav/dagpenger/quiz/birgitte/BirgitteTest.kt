package no.nav.dagpenger.quiz.birgitte

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BirgitteTest {
    lateinit var rapid: TestRapid

    @BeforeEach
    fun setup() {
        rapid = TestRapid().apply {
            Birgitte(rapidsConnection = this)
        }
    }

    @Test
    fun `skal mappe løsninger til faktum`() {
        rapid.sendTestMessage(løstBehovJson)

        with(rapid.inspektør) {
            assertEquals(1, size)

            field(0, "fakta").forEach {
                assertNotNull(it["svar"])
            }
        }
    }

    @Test
    fun `skal mappe komplekse løsninger til faktum`() {
        rapid.sendTestMessage(løstBehovMedKompleksLøsningJson)

        with(rapid.inspektør) {
            assertEquals(1, size)

            field(0, "fakta").forEach {
                assertNotNull(it["svar"])
            }
        }
    }
}

@Language("json")
private val løstBehovJson = """
{
  "@event_name": "behov",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "fnr": "123",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "fakta": [ 
    {
      "behov": "Registreringsdato"
    },
    {
      "behov": "Fødselsdato"
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
}""".trimIndent()

@Language("json")
private val løstBehovMedKompleksLøsningJson = """
{
  "@event_name": "behov",
  "@opprettet": "2020-11-18T11:04:32.867824",
  "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
  "fnr": "123",
  "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
  "fakta": [ 
    {
      "behov": "Registreringsperiode"
    }
  ],
  "@behov": [
    "Registreringsperiode"
  ],
  "@løsning": {
    "Registreringsperiode": {
      "start": "2020-01-01",
      "slutt": "2020-12-12"
    }
  },
  "Søknadstidspunkt": "2020-11-09"
}""".trimIndent()