package duels.poker.server.http

import duels.poker.server.module
import duels.poker.server.protocol.http.DuelOutcomeLabel
import duels.poker.server.protocol.http.DuelSummaryResponse
import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.protocol.http.RecentDuelsResponse
import duels.poker.server.protocol.http.duelSummaryResponse
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.protocol.protocolJson
import duels.poker.server.session.PlayerId
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class ProfileRouteTest {
    @Test
    fun aKnownDeviceGetsItsProfile() = testApplication {
        val reads = FakeProfileReads(
            mapOf(
                "alice" to profileResponse("p-alice", 4),
            ),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"playerId\":\"p-alice\""))
        assertTrue(body.contains("\"coinBalance\":4"))
        assertTrue(reads.queried.contains("p-alice"))
    }

    @Test
    fun aNegativeBalanceIsReturnedUnclamped() = testApplication {
        val reads = FakeProfileReads(
            mapOf(
                "bob" to profileResponse("p-bob", -3),
            ),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "bob")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"coinBalance\":-3"))
    }

    @Test
    fun anAbsentDeviceIdHeaderIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me")
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(reads.queried.isEmpty())
    }

    @Test
    fun aBlankDeviceIdHeaderIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "  ")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(reads.queried.isEmpty())
    }

    @Test
    fun anUnknownDeviceIdIsRefused() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun aMalformedAuthorizationHeaderIsRefusedNotDowngradedToTheDevice() = testApplication {
        // TASK-040510's review, made concrete one layer up: a header that does not parse as
        // `Bearer <token>` must still reach the resolver as a *presented*, invalid token — never
        // silently become "no token", which would let this device, resolvable and sitting right
        // beside it, answer instead of a 401. The device is the positive control: without it,
        // this test could not tell a real fix from a fixture that would have answered 401 anyway.
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 4)))
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me") {
            header(HttpHeaders.Authorization, "not-a-bearer-token")
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertEquals("", response.bodyAsText())
        assertTrue(reads.queried.isEmpty())
    }

    @Test
    fun aKnownDeviceGetsItsDuelsInTheOrderTheReaderReturnedThem() = testApplication {
        val duel1 = duelSummaryResponse(
            duelId = "duel-1",
            opponentPlayerId = "p-bob",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 10,
            finishedAt = "2026-08-12T10:00:00Z",
        )
        val duel2 = duelSummaryResponse(
            duelId = "duel-2",
            opponentPlayerId = "p-charlie",
            outcome = DuelOutcomeLabel.LOST,
            coinDelta = -1,
            handsPlayed = 5,
            finishedAt = "2026-08-12T09:00:00Z",
        )
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to listOf(duel1, duel2)),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"duelId\":\"duel-1\""))
        assertTrue(body.contains("\"duelId\":\"duel-2\""))
        assertTrue(body.indexOf("duel-1") < body.indexOf("duel-2"))
    }

    @Test
    fun aDuelLineOnTheWireCarriesTheOpponentsName() = testApplication {
        val duel = duelSummaryResponse(
            duelId = "duel-3",
            opponentPlayerId = "p-torvald",
            outcome = DuelOutcomeLabel.WON,
            coinDelta = 1,
            handsPlayed = 8,
            finishedAt = "2026-08-14T15:00:00Z",
            opponentDisplayName = "Torvald",
        )
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to listOf(duel)),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(""""opponentDisplayName":"Torvald"""))
    }

    @Test
    fun aDuelLineForAnUnnamedOpponentCarriesTheFieldAsNull() = testApplication {
        val duel = duelSummaryResponse(
            duelId = "duel-4",
            opponentPlayerId = "p-unnamed",
            outcome = DuelOutcomeLabel.DREW,
            coinDelta = 0,
            handsPlayed = 6,
            finishedAt = "2026-08-14T16:00:00Z",
            opponentDisplayName = null,
        )
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to listOf(duel)),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertTrue(response.bodyAsText().contains(""""opponentDisplayName":null"""))
    }

    @Test
    fun anAbsentLimitAsksForTheDefault() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        // The probe: one row more than the client-facing default, so the route can tell whether a
        // following page exists without a second round trip.
        assertEquals(DEFAULT_DUEL_LIMIT + 1, reads.lastLimitRequested)
    }

    @Test
    fun aRequestWithNoCursorAsksForNone() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(null), reads.cursorsRequested)
        }
    }

    @Test
    fun aRequestWithNoFilterAsksForNone() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            // Not isEmpty(): that would also pass for a route that never called the port at all.
            // The singleton list is what proves the port was asked exactly once, with the
            // no-op filter, rather than the route inventing a filter of its own (ADR-0002).
            assertEquals(listOf(DuelFilter.NONE), reads.filtersRequested)
        }
    }

    @Test
    fun aLimitAboveTheCapIsClamped() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels?limit=999") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        // The cap still governs what is returned; the probe asks for one more than the clamped
        // value, so the port sees the cap plus one, not the cap itself.
        assertEquals(MAX_DUEL_LIMIT + 1, reads.lastLimitRequested)
    }

    @Test
    fun aNonNumericLimitIsABadRequest() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels?limit=abc") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertEquals(0, reads.lastLimitRequested)
    }

    @Test
    fun aPlayerWithNoDuelsGetsAnEmptyListAndTwoHundred() = testApplication {
        val reads = FakeProfileReads(
            mapOf("alice" to profileResponse("p-alice", 0)),
            mapOf("p-alice" to emptyList()),
        )
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels") {
            header(DEVICE_ID_HEADER, "alice")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"duels\":[]"))
    }

    @Test
    fun anUnknownDeviceIsRefusedBeforeTheLimitIsParsed() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        application {
            module()
            profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
        }
        val response = client.get("/api/me/duels?limit=abc") {
            header(DEVICE_ID_HEADER, "ghost")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
    }

    @Test
    fun aCursorFromTheQueryReachesThePort() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val cursor =
                DuelCursor(
                    Instant.parse("2026-08-12T09:00:00Z"),
                    UUID.fromString("11111111-1111-1111-1111-111111111111"),
                )
            val response = client.get("/api/me/duels?after=${cursor.encoded(DuelFilter.NONE)}") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(cursor), reads.cursorsRequested)
        }
    }

    @Test
    fun aMalformedCursorIsABadRequestAndReadsNothing() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?after=not-a-cursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun anEmptyCursorValueIsABadRequest() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?after=") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun anUnknownDeviceIsRefusedBeforeTheCursorIsParsed() {
        testApplication {
            val reads = FakeProfileReads(emptyMap())
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?after=not-a-cursor") {
                header(DEVICE_ID_HEADER, "ghost")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun anOutcomeParameterReachesThePort() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?outcome=WON") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(DuelFilter(DuelOutcomeLabel.WON, null)), reads.filtersRequested)
        }
    }

    @Test
    fun anOpponentParameterReachesThePort() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?opponent=Halvard") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(DuelFilter(null, "Halvard")), reads.filtersRequested)
        }
    }

    @Test
    fun anUnusableOutcomeIsABadRequestAndReadsNothing() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?outcome=won") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(reads.filtersRequested.isEmpty())
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun anEmptyOpponentValueIsABadRequest() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?opponent=") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(reads.filtersRequested.isEmpty())
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun anUnknownDeviceIsRefusedBeforeTheFilterIsParsed() {
        testApplication {
            val reads = FakeProfileReads(emptyMap())
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?outcome=won")
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(reads.filtersRequested.isEmpty())
            assertTrue(reads.cursorsRequested.isEmpty())
        }
    }

    @Test
    fun aFullPageReportsTheCursorOfItsLastRow() {
        testApplication {
            val duel1 = duelSummaryResponse(
                duelId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 10,
                finishedAt = "2026-08-15T13:00:00Z",
            )
            val duel2 = duelSummaryResponse(
                duelId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 8,
                finishedAt = "2026-08-15T12:00:00Z",
            )
            val duel3 = duelSummaryResponse(
                duelId = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 6,
                finishedAt = "2026-08-15T11:00:00Z",
            )
            // The probe: index `limit`, present in the fixture so a naive implementation that
            // cursors off it — instead of duel3 — would be caught by the assertion below.
            val duel4 = duelSummaryResponse(
                duelId = "dddddddd-dddd-dddd-dddd-dddddddddddd",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 4,
                finishedAt = "2026-08-15T10:00:00Z",
            )
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to listOf(duel1, duel2, duel3, duel4)),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?limit=3") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val decoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), response.bodyAsText())
            assertEquals(listOf(duel1.duelId, duel2.duelId, duel3.duelId), decoded.duels.map { it.duelId })
            // This is the off-by-one that silently loses a duel: a cursor built from duel4 (the
            // probe) instead of duel3 (the page's real last row) would still look like a cursor
            // here — only decoding it and comparing the value catches the difference.
            val expectedCursor = DuelCursor(Instant.parse(duel3.finishedAt), UUID.fromString(duel3.duelId))
            assertEquals(expectedCursor, decoded.nextCursor?.let { duelCursorOrNull(it, DuelFilter.NONE) })
        }
    }

    @Test
    fun anExactlyFullRecordReportsNoNextPage() {
        testApplication {
            val duel1 = duelSummaryResponse(
                duelId = "duel-1",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 10,
                finishedAt = "2026-08-15T13:00:00Z",
            )
            val duel2 = duelSummaryResponse(
                duelId = "duel-2",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 8,
                finishedAt = "2026-08-15T12:00:00Z",
            )
            val duel3 = duelSummaryResponse(
                duelId = "duel-3",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 6,
                finishedAt = "2026-08-15T11:00:00Z",
            )
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to listOf(duel1, duel2, duel3)),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            // The fake holds exactly `limit` rows: the probe finds nothing. A naive
            // returned == limit ⇒ there is more would get this wrong.
            val response = client.get("/api/me/duels?limit=3") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val decoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), response.bodyAsText())
            assertEquals(listOf(duel1.duelId, duel2.duelId, duel3.duelId), decoded.duels.map { it.duelId })
            assertEquals(null, decoded.nextCursor)
        }
    }

    @Test
    fun aShortPageReportsNoNextPage() {
        testApplication {
            val duel1 = duelSummaryResponse(
                duelId = "duel-1",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 10,
                finishedAt = "2026-08-15T13:00:00Z",
            )
            val duel2 = duelSummaryResponse(
                duelId = "duel-2",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 8,
                finishedAt = "2026-08-15T12:00:00Z",
            )
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to listOf(duel1, duel2)),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?limit=3") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val decoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), response.bodyAsText())
            assertEquals(listOf(duel1.duelId, duel2.duelId), decoded.duels.map { it.duelId })
            assertEquals(null, decoded.nextCursor)
        }
    }

    @Test
    fun anEmptyRecordReportsNoNextPage() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to emptyList()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val response = client.get("/api/me/duels?limit=3") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals("""{"duels":[],"nextCursor":null}""", response.bodyAsText())
        }
    }

    @Test
    fun followingTheReturnedCursorBeginsAtTheRowThatWasTheProbe() {
        testApplication {
            // The route asks for limit + 1 rows to learn, without a second query, whether another
            // page exists. If nextCursor were built from that extra probe row instead of from the
            // page's real last row, every following request would start one row too late: a duel
            // silently skipped on every page boundary, forever, with nothing about either response
            // looking wrong on its own. This test is the one that would catch it — by actually
            // following the cursor.
            val duel1 = duelSummaryResponse(
                duelId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 10,
                finishedAt = "2026-08-15T13:00:00Z",
            )
            val duel2 = duelSummaryResponse(
                duelId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 8,
                finishedAt = "2026-08-15T12:00:00Z",
            )
            val duel3 = duelSummaryResponse(
                duelId = "cccccccc-cccc-cccc-cccc-cccccccccccc",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 6,
                finishedAt = "2026-08-15T11:00:00Z",
            )
            // The probe on the first request. It must never be skipped by the second request.
            val duel4 = duelSummaryResponse(
                duelId = "dddddddd-dddd-dddd-dddd-dddddddddddd",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 4,
                finishedAt = "2026-08-15T10:00:00Z",
            )
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to listOf(duel1, duel2, duel3, duel4)),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val firstPage = client.get("/api/me/duels?limit=3") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, firstPage.status)
            val firstDecoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), firstPage.bodyAsText())
            val nextCursor = requireNotNull(firstDecoded.nextCursor) { "a page smaller than the fixture must carry a cursor" }

            val secondPage = client.get("/api/me/duels?limit=3&after=$nextCursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, secondPage.status)
            val secondDecoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), secondPage.bodyAsText())
            // If nextCursor had named duel4 (the probe) instead of duel3 (the real last row), the
            // fake's cursor filtering — which mirrors "give me rows after this one" — would find
            // nothing past duel4, and this page would come back empty: duel4 missing, permanently.
            // Its presence here, first, is exactly what proves the cursor named duel3.
            assertEquals(listOf(duel4.duelId), secondDecoded.duels.map { it.duelId })
            assertEquals(null, secondDecoded.nextCursor)
        }
    }

    @Test
    fun aCursorFollowedUnderTheSameFilterPagesOn() {
        testApplication {
            val duels = twoDuels()
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to duels),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val firstPage = client.get("/api/me/duels?limit=1&outcome=WON") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, firstPage.status)
            val firstDecoded = protocolJson.decodeFromString(RecentDuelsResponse.serializer(), firstPage.bodyAsText())
            val mintedCursor =
                requireNotNull(firstDecoded.nextCursor) { "limit=1 against two rows must carry a nextCursor" }

            // The replay is under the exact filter the cursor was minted under — the round trip
            // this ticket exists to prove works, not merely refuse (ADR-0057 §1).
            val secondPage = client.get("/api/me/duels?limit=1&outcome=WON&after=$mintedCursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, secondPage.status)
            val secondDecoded =
                protocolJson.decodeFromString(RecentDuelsResponse.serializer(), secondPage.bodyAsText())
            assertEquals(listOf(duels[1].duelId), secondDecoded.duels.map { it.duelId })

            // The decoded value, not merely that a cursor arrived: a route minting under
            // DuelFilter.NONE regardless of the request's filter would already have failed the OK
            // assertion above, so this pins the row the cursor actually names.
            val row = duels[0]
            val expectedCursor = DuelCursor(Instant.parse(row.finishedAt), UUID.fromString(row.duelId))
            assertEquals(expectedCursor, reads.cursorsRequested.last())
        }
    }

    @Test
    fun aCursorFromOneFilterIsRefusedUnderAnother() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to twoDuels()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val mintingPage = client.get("/api/me/duels?limit=1&outcome=WON") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, mintingPage.status)
            val mintingDecoded =
                protocolJson.decodeFromString(RecentDuelsResponse.serializer(), mintingPage.bodyAsText())
            val mintedCursor =
                requireNotNull(mintingDecoded.nextCursor) { "limit=1 against two rows must carry a nextCursor" }

            // The cursor above was minted under WON; replayed under LOST it must be refused — the
            // silent reinterpretation ADR-0057 exists to close.
            val response = client.get("/api/me/duels?limit=1&outcome=LOST&after=$mintedCursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            // The minting request legitimately read once; the refused request must not add a
            // second entry.
            assertEquals(1, reads.cursorsRequested.size)
        }
    }

    @Test
    fun anUnfilteredCursorIsRefusedUnderAFilter() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to twoDuels()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val mintingPage = client.get("/api/me/duels?limit=1") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, mintingPage.status)
            val mintingDecoded =
                protocolJson.decodeFromString(RecentDuelsResponse.serializer(), mintingPage.bodyAsText())
            val mintedCursor =
                requireNotNull(mintingDecoded.nextCursor) { "limit=1 against two rows must carry a nextCursor" }

            // DuelFilter.NONE must fingerprint too, or an unfiltered cursor would open any
            // filtered walk.
            val response = client.get("/api/me/duels?limit=1&outcome=WON&after=$mintedCursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(1, reads.cursorsRequested.size)
        }
    }

    @Test
    fun aFilteredCursorIsRefusedWithNoFilter() {
        testApplication {
            val reads = FakeProfileReads(
                mapOf("alice" to profileResponse("p-alice", 0)),
                mapOf("p-alice" to twoDuels()),
            )
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(), identitiesFor(reads.profiles))
            }
            val mintingPage = client.get("/api/me/duels?limit=1&opponent=Halvard") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.OK, mintingPage.status)
            val mintingDecoded =
                protocolJson.decodeFromString(RecentDuelsResponse.serializer(), mintingPage.bodyAsText())
            val mintedCursor =
                requireNotNull(mintingDecoded.nextCursor) { "limit=1 against two rows must carry a nextCursor" }

            // The same bug pointed the other way: fingerprinting only when a filter is present
            // would pass the minting half above and let this replay through.
            val response = client.get("/api/me/duels?limit=1&after=$mintedCursor") {
                header(DEVICE_ID_HEADER, "alice")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals(1, reads.cursorsRequested.size)
        }
    }

    @Test
    fun aKnownDeviceSetsItsName() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 4)))
        val writes = FakeProfileWrites(SetNameResult.NameSet(profileResponse("p-alice", 4, "Alice")))
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Alice"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"playerId\":\"p-alice\""))
        assertTrue(body.contains("\"coinBalance\":4"))
        assertTrue(body.contains("\"displayName\":\"Alice\""))
    }

    @Test
    fun theCanonicalNameIsWhatReachesThePort() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites(SetNameResult.NameSet(profileResponse("p-alice", 0, "Bob")))
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"  Bob  "}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        assertEquals("Bob", writes.received.single())
    }

    @Test
    fun anAbsentDeviceIdIsRefusedBeforeTheBodyIsRead() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        // A valid body, so a wrong implementation that answered on the body would answer 200,
        // not 401 — the identity refusal below, not the body one.
        val response = client.put("/api/me/name") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun anAbsentDeviceIdIsRefusedBeforeAMalformedBodyIsRead() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        // A body that cannot even decode: if identity were checked after the body were read,
        // this would answer 400 — the same wrong answer the body's own defect would produce —
        // instead of 401. This is what tells "identity first" apart from "identity checked
        // before the port is called, but only after a harmless body is silently discarded".
        val response = client.put("/api/me/name") {
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"nickname":"bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun anUnknownDeviceIdIsRefusedBeforeTheNameIsSet() = testApplication {
        val reads = FakeProfileReads(emptyMap())
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "ghost")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Bob"}""")
        }
        assertEquals(HttpStatusCode.Unauthorized, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun aNameTheRulesRefuseIsABadRequest() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"  "}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun aBodyThatIsNotTheRequestShapeIsABadRequest() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites()
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"nickname":"bob"}""")
        }
        assertEquals(HttpStatusCode.BadRequest, response.status)
        assertTrue(writes.received.isEmpty())
    }

    @Test
    fun aNameSomebodyElseHoldsIsAConflict() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites(SetNameResult.NameTaken)
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Bob"}""")
        }
        assertEquals(HttpStatusCode.Conflict, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun aPlayerWhoAlreadyHasANameIsForbidden() = testApplication {
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
        val writes = FakeProfileWrites(SetNameResult.AlreadyNamed)
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Charlie"}""")
        }
        assertEquals(HttpStatusCode.Forbidden, response.status)
        assertEquals("", response.bodyAsText())
    }

    @Test
    fun theTwoRefusalsAreDifferentStatuses() {
        // Test both result types and verify they produce different status codes.
        // This catches a mapping that incorrectly treats them the same.
        var takenStatus: HttpStatusCode? = null
        testApplication {
            val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(SetNameResult.NameTaken), identitiesFor(reads.profiles))
            }
            val response = client.put("/api/me/name") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"name":"Bob"}""")
            }
            takenStatus = response.status
        }

        var alreadyNamedStatus: HttpStatusCode? = null
        testApplication {
            val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            application {
                module()
                profileRoutes(reads, FakeProfileWrites(SetNameResult.AlreadyNamed), identitiesFor(reads.profiles))
            }
            val response = client.put("/api/me/name") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"name":"Bob"}""")
            }
            alreadyNamedStatus = response.status
        }

        assertEquals(HttpStatusCode.Conflict, takenStatus)
        assertEquals(HttpStatusCode.Forbidden, alreadyNamedStatus)
        // Verify they're different — a mapping collapsing both to one status fails here
        assertTrue(takenStatus != alreadyNamedStatus)
    }

    @Test
    fun aResentIdenticalNameIsStillTwoHundred() = testApplication {
        val profile = profileResponse("p-alice", 2, "Alice")
        val reads = FakeProfileReads(mapOf("alice" to profileResponse("p-alice", 2)))
        val writes = FakeProfileWrites(SetNameResult.NameSet(profile))
        application {
            module()
            profileRoutes(reads, writes, identitiesFor(reads.profiles))
        }
        val response = client.put("/api/me/name") {
            header(DEVICE_ID_HEADER, "alice")
            header(HttpHeaders.ContentType, "application/json")
            setBody("""{"name":"Alice"}""")
        }
        assertEquals(HttpStatusCode.OK, response.status)
        val body = response.bodyAsText()
        assertTrue(body.contains("\"playerId\":\"p-alice\""))
        assertTrue(body.contains("\"coinBalance\":2"))
        assertTrue(body.contains("\"displayName\":\"Alice\""))
    }

    // Two rows and `limit=1`, so the first page always carries a nextCursor to hand back.
    private fun twoDuels(): List<DuelSummaryResponse> =
        listOf(
            duelSummaryResponse(
                duelId = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.WON,
                coinDelta = 1,
                handsPlayed = 10,
                finishedAt = "2026-08-15T13:00:00Z",
            ),
            duelSummaryResponse(
                duelId = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb",
                opponentPlayerId = "p-bob",
                outcome = DuelOutcomeLabel.LOST,
                coinDelta = -1,
                handsPlayed = 8,
                finishedAt = "2026-08-15T12:00:00Z",
            ),
        )

    private class FakeProfileReads(
        val profiles: Map<String, ProfileResponse>,
        private val duels: Map<String, List<DuelSummaryResponse>> = emptyMap(),
    ) : ProfileReads {
        val queried: MutableList<String> = mutableListOf()
        val cursorsRequested: MutableList<DuelCursor?> = mutableListOf()
        val filtersRequested: MutableList<DuelFilter> = mutableListOf()
        var lastLimitRequested: Int = 0

        override suspend fun profileOf(playerId: PlayerId): ProfileResponse? {
            queried.add(playerId.value)
            return profiles.values.find { it.playerId == playerId.value }
        }

        override suspend fun recentDuelsOf(
            playerId: PlayerId,
            limit: Int,
            after: DuelCursor?,
            filter: DuelFilter,
        ): List<DuelSummaryResponse> {
            lastLimitRequested = limit
            cursorsRequested.add(after)
            filtersRequested.add(filter)
            val all = duels[playerId.value] ?: emptyList()
            if (after == null) return all
            // Mirrors the real port's "rows after this cursor" contract closely enough for a
            // route-level test: find the fixture's own row named by the cursor, and return what
            // comes after it in the fixture's order. This is what lets
            // followingTheReturnedCursorBeginsAtTheRowThatWasTheProbe actually catch a cursor
            // built from the wrong row — a fake that ignored the cursor's value could not.
            val cursorRowIndex = all.indexOfFirst { row ->
                Instant.parse(row.finishedAt) == after.finishedAt && UUID.fromString(row.duelId) == after.duelId
            }
            return if (cursorRowIndex == -1) all else all.drop(cursorRowIndex + 1)
        }
    }

    private class FakeProfileWrites(
        private val result: SetNameResult = SetNameResult.NameTaken,
    ) : ProfileWrites {
        val received: MutableList<String> = mutableListOf()

        override suspend fun setDisplayName(playerId: PlayerId, canonicalName: String): SetNameResult {
            received.add(canonicalName)
            return result
        }
    }
}
