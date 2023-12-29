package no.nav.dagpenger.quiz.birgitte

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

internal class BirgitteTest {
    lateinit var rapid: TestRapid

    @BeforeEach
    fun setup() {
        rapid =
            TestRapid().apply {
                Birgitte(rapidsConnection = this)
            }
    }

    @Test
    fun `skal mappe løsninger til faktum`() {
        rapid.sendTestMessage(løstBehovJson)

        with(rapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@final"))

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

    @Test
    fun `skal mappe generator faktum (lister) `() {
        rapid.sendTestMessage(løstBehovMedGeneratorFaktum)
        with(rapid.inspektør) {
            assertEquals(1, size)

            field(0, "fakta").forEach {
                assertNotNull(it["svar"])
                assertTrue(it["svar"].isArray)
                assertEquals(2, it["svar"].size())
                it["svar"].forEach { svar ->
                    assertEquals(2, svar.size())
                    svar.forEach { faktum ->
                        assertTrue(faktum.has("id"))
                        assertTrue(faktum.has("svar"))
                        assertTrue(faktum.has("type"))
                    }
                }
            }
        }
    }
}

@Language("json")
private val løstBehovJson =
    """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
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
private val løstBehovMedKompleksLøsningJson =
    """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [ 
        {
           "id": "10",
          "behov": "komplekst",
          "type": "localdate"
        }
      ],
      "@behov": [
        "komplekst"
      ],
      "@løsning": {
        "komplekst": {
          "start": "2020-01-01",
          "slutt": "2020-12-12"
        }
      },
      "Søknadstidspunkt": "2020-11-09"
    }
    """.trimIndent()

@Language("json")
private val løstBehovMedGeneratorFaktum =
    """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "fnr": "123",
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "fakta": [
        {
          "id": "10",
          "behov": "Registreringsperioder",
          "type": "generator",
          "templates": [
            {
              "id": "11",
              "navn": "fom",
              "type": "localdate"
            },
            {
              "id": "12",
              "navn": "tom",
              "type": "localdate"
            }
          ]
        }
      ],
      "@behov": [
        "Registreringsperioder"
      ],
      "Søknadstidspunkt": "2020-11-09",
      "@løsning": {
        "Registreringsperioder": [
          {
            "fom": "2020-01-01",
            "tom": "2020-01-08"
          },
          {
            "fom": "2020-01-09",
            "tom": "2020-01-16"
          }
        ]
      }
    }
    """.trimIndent()
