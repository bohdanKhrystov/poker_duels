package duels.poker.server.http

import duels.poker.server.auth.AttemptBudget
import duels.poker.server.auth.AttemptLimits
import duels.poker.server.auth.AuthSessions
import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.Credentials
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.auth.SessionToken
import duels.poker.server.module
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.session.PlayerId
import duels.poker.server.time.MutableClock
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.ApplicationCallPipeline
import io.ktor.server.application.call
import io.ktor.server.plugins.mutableOriginConnectionPoint
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthRouteTest {
    @Test
    fun anAbsentDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // A well-formed, entirely valid body: a wrong implementation that reached the guard
            // or the write would answer 201/409, not 401 — this 401 can only have come from the
            // identity step.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBlankDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // A header of only whitespace: DeviceId's init rejects a blank value, so without the
            // isNotBlank guard this would throw and answer 500, not 401.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "   ")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun anUnknownDeviceIdIsRefused() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // A malformed body: a wrong implementation that only checked the header's presence
            // before decoding, deferring the profile lookup until after, would answer 400 here —
            // the same wrong answer a stranger's own malformed body would produce — instead of
            // 401.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "ghost")
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun anUndecodableBodyIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBodyMissingThePasswordIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aBodyCarryingAPlayerIdIsFourHundred() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // An unrecognised field: a client cannot assert an identity even by trying, because
            // SignUpRequest has no playerId field to decode one into.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222","playerId":"p-mallory"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theBodyIsNeverDecodedBeforeIdentity() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // The single most important test in the ticket: no device id, and a body that cannot
            // even decode. The wrong implementation this must fail against is one that decodes
            // first — that implementation would answer 400 here, telling a stranger their body,
            // not their missing identity, was the problem.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("not json")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theFieldsAreNeverJudgedBeforeIdentity() {
        testApplication {
            val reads = FixedProfileReads(emptyMap())
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // The body decodes fine, but its handle fails signUpFieldsOf's own rule, which would
            // answer 400 once identity is known. A wrong implementation that judged fields before
            // confirming identity would answer 400 here instead of 401, telling a stranger their
            // handle, not their missing identity, was the problem.
            val response = client.post("/api/auth/sign-up") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aSignUpAnswersCreated() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals("", response.bodyAsText())
        }
    }

    @Test
    fun theCreateCallCarriesTheResolvedPlayerAndTheFoldedHandle() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // Bob_1 changes under the fold, so a handler that skipped folding would fail here even
            // though it would pass with an already-lowercase handle. The player id must be the
            // server's own p-alice — never the device id alice, and never a body field, because
            // SignUpRequest has none to carry one.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"Bob_1","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(1, credentials.createCalls.size)
            val call = credentials.createCalls[0]
            assertEquals(PlayerId("p-alice"), call.playerId)
            assertEquals(CredentialKind.PASSWORD, call.kind)
            assertEquals("bob_1", call.identifier)
            // PresentedSecret redacts toString, so the secret is compared by its value directly.
            assertEquals("hunter2222", call.secret.value)
        }
    }

    @Test
    fun aPlayerWhoAlreadyHoldsAPasswordIsRefused() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials(holds = true)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // holds = true: the guard alone must stop the write. createCalls staying empty proves
            // no Argon2 work was spent (ADR-0030 §1); holdsCalls being non-empty proves the guard
            // was actually the thing that answered, not skipped in favour of some other check.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
            assertTrue(credentials.holdsCalls.isNotEmpty())
            assertTrue(credentials.createCalls.isEmpty())
        }
    }

    @Test
    fun aTakenHandleIsRefused() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials =
                RecordingCredentials(createResult = CreateCredentialResult.IdentifierTaken)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // The guard passes (holds = false, the default) but the write itself reports the
            // identifier taken — the same 409 as the case above, reached by a different branch.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Conflict, response.status)
        }
    }

    @Test
    fun aRefusedHandleReachesNeitherPortFunction() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            // "ab" fails signUpFieldsOf's own rule, after identity and decoding both already
            // succeeded. A refusal that still costs a round trip to either port is a refusal that
            // leaks work.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"ab","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun aPasswordOutsideTheBoundsReachesNeitherPortFunction() {
        // Two inputs, one per bound: a password one code point under the floor, and one code
        // point over the ceiling.
        val tooShort = "a".repeat(7)
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"$tooShort"}""")
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }

        val tooLong = "a".repeat(129)
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"$tooLong"}""")
            }
            assertEquals(HttpStatusCode.UnprocessableEntity, response.status)
            assertTrue(credentials.createCalls.isEmpty())
            assertTrue(credentials.holdsCalls.isEmpty())
        }
    }

    @Test
    fun theGuardIsAskedBeforeCreate() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, freshSignUpBudget())
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
            assertEquals(1, credentials.holdsCalls.size)
            assertEquals(PlayerId("p-alice") to CredentialKind.PASSWORD, credentials.holdsCalls[0])
        }
    }

    @Test
    fun theSixthSignUpFromOneAddressIsFourHundredAndTwentyNine() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            val budget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            // Five requests spend the whole budget; a route that never checked it would answer
            // 201 a sixth time too, which is exactly the attack ADR-0055 §1 closes.
            repeat(5) {
                val response = client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"handle":"bob","password":"hunter2222"}""")
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
            val sixth = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.TooManyRequests, sixth.status)
            assertEquals("", sixth.bodyAsText())
        }
    }

    @Test
    fun anOverBudgetSignUpWritesNothing() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            val budget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            repeat(5) {
                client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"handle":"bob","password":"hunter2222"}""")
                }
            }
            // Five admitted requests, five create calls — one each, proven by a count rather than
            // a boolean, and recorded before the sixth is sent so its own contribution is isolated
            // below.
            assertEquals(5, credentials.createCalls.size)
            val sixth = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.TooManyRequests, sixth.status)
            // The over-budget request added no create call: still five, not six. No hash ran.
            assertEquals(5, credentials.createCalls.size)
        }
    }

    @Test
    fun aRefusedSignUpSpendsNoBudget() {
        // This is the ticket's real assertion: a budget checked in the wrong place — before the
        // holdsCredential guard rather than after it — passes every other test in this class.
        val budget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials(holds = true)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            // Five requests that all stop at the guard, before the budget is ever consulted.
            repeat(5) {
                val response = client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"handle":"bob","password":"hunter2222"}""")
                }
                assertEquals(HttpStatusCode.Conflict, response.status)
            }
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("carol" to profileResponse("p-carol", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            // The same shared budget, a sixth request, this time reaching create: 201, not 429,
            // proves the five 409s above spent none of the five slots.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "carol")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"dave","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun aMalformedBodySpendsNoBudget() {
        // The same shape as aRefusedSignUpSpendsNoBudget, with five 400s in front instead of five
        // 409s: a decode failure must be exactly as free as a guard refusal.
        val budget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            repeat(5) {
                val response = client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("not json")
                }
                assertEquals(HttpStatusCode.BadRequest, response.status)
            }
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("carol" to profileResponse("p-carol", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "carol")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"dave","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun twoAddressesHaveTwoBudgets() {
        // AttemptBudget is keyed by call.request.origin.remoteAddress, which Ktor's test client
        // always reports as "localhost" — so each block below installs its own intercept that
        // overwrites the call's origin before authRoutes ever sees it. AuthRoutes.kt itself reads
        // only call.request.origin.remoteAddress, exactly as production does; this is a test-side
        // stand-in for two different clients, not a second header the route is aware of.
        val budget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())

        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.10"
                }
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            // Five requests exhaust this address's whole budget.
            repeat(5) {
                val response = client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"handle":"bob","password":"hunter2222"}""")
                }
                assertEquals(HttpStatusCode.Created, response.status)
            }
        }

        testApplication {
            val reads = FixedProfileReads(mapOf("carol" to profileResponse("p-carol", 0)))
            val credentials = RecordingCredentials()
            application {
                module()
                intercept(ApplicationCallPipeline.Plugins) {
                    call.mutableOriginConnectionPoint.remoteAddress = "203.0.113.20"
                }
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            // A different address's first request, against the very same budget instance, still
            // reaches create — proving the key that separates them was actually read from the
            // request rather than being a single counter shared by everyone.
            val response = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "carol")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"dave","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.Created, response.status)
        }
    }

    @Test
    fun anOverBudgetRequestStillCounts() {
        testApplication {
            val reads = FixedProfileReads(mapOf("alice" to profileResponse("p-alice", 0)))
            val credentials = RecordingCredentials()
            val clock = MutableClock()
            val budget = AttemptBudget(AttemptLimits(5, 900_000L), clock)
            application {
                module()
                authRoutes(reads, credentials, identitiesFor(reads.profiles), NoAuthSessions, budget)
            }
            repeat(5) {
                client.post("/api/auth/sign-up") {
                    header(DEVICE_ID_HEADER, "alice")
                    header(HttpHeaders.ContentType, "application/json")
                    setBody("""{"handle":"bob","password":"hunter2222"}""")
                }
            }
            val sixth = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.TooManyRequests, sixth.status)

            // Still inside the original fifteen-minute window measured from the first request: a
            // budget that discarded the sixth's own over-budget attempt instead of recording it
            // would show nothing new here, but one that quietly reset the whole key on refusal
            // would wrongly admit this seventh request — hammering must extend, never reset.
            clock.advance(899_999L)
            val seventh = client.post("/api/auth/sign-up") {
                header(DEVICE_ID_HEADER, "alice")
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.TooManyRequests, seventh.status)
        }
    }

    @Test
    fun aCorrectCredentialAnswersTwoHundredAndAToken() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("hunter2222" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            // The exact token the double issued, read back from the double itself rather than
            // hard-coded here, so a route that echoed some other string could not pass by accident.
            val issuedToken = sessions.issued.single().second
            assertEquals("""{"sessionToken":"${issuedToken.value}"}""", response.bodyAsText())
        }
    }

    @Test
    fun theTokenNamesThePlayerTheCredentialNamed() {
        // Two credentials resolving to two different players, both driven below: a route that
        // issued for a hard-coded player would still pass a single-credential version of this
        // test, which is exactly why it drives two.
        val alicePlayerId = PlayerId("p-alice")
        val bobPlayerId = PlayerId("p-bob")
        val credentials = SignInCredentials(
            mapOf(
                "alice" to ("alicepassword1" to alicePlayerId),
                "bob" to ("bobpassword1" to bobPlayerId),
            ),
        )

        testApplication {
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"alicepassword1"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(alicePlayerId), sessions.issued.map { it.first })
            assertEquals(alicePlayerId, sessions.playerOf(sessions.issued.single().second))
        }

        testApplication {
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"bob","password":"bobpassword1"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            assertEquals(listOf(bobPlayerId), sessions.issued.map { it.first })
            assertEquals(bobPlayerId, sessions.playerOf(sessions.issued.single().second))
        }
    }

    @Test
    fun aWrongPasswordAndAnUnknownHandleAreIndistinguishable() {
        val credentials = SignInCredentials(mapOf("alice" to ("correctpassword1" to PlayerId("p-alice"))))

        lateinit var wrongPassword: SignInOutcome
        testApplication {
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    RecordingAuthSessions(),
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"wrongpassword1"}""")
            }
            wrongPassword = SignInOutcome(response.status, response.bodyAsText(), response.headers.names())
        }

        lateinit var unknownHandle: SignInOutcome
        testApplication {
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    RecordingAuthSessions(),
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"ghost","password":"whatever1"}""")
            }
            unknownHandle = SignInOutcome(response.status, response.bodyAsText(), response.headers.names())
        }

        // Two separate "is 401" checks would still pass for a route that told the two cases apart
        // by body or header; comparing the outcomes to each other is what a stranger actually sees.
        assertEquals(HttpStatusCode.Unauthorized, wrongPassword.status)
        assertEquals(wrongPassword.status, unknownHandle.status)
        assertEquals(wrongPassword.body, unknownHandle.body)
        assertEquals(wrongPassword.headerNames, unknownHandle.headerNames)
    }

    @Test
    fun anUnusableHandleAnswersTheSameFourHundredAndOne() {
        testApplication {
            val credentials = SignInCredentials(emptyMap())
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    RecordingAuthSessions(),
                    freshSignUpBudget(),
                )
            }
            // "!!" fails loginHandleOrNull's own character rule before any credential is consulted.
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"!!","password":"whatever1"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.verifyCalls.isEmpty())
        }
    }

    @Test
    fun aMalformedBodyIsFourHundred() {
        testApplication {
            val credentials = SignInCredentials(emptyMap())
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    RecordingAuthSessions(),
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice"}""")
            }
            assertEquals(HttpStatusCode.BadRequest, response.status)
            assertEquals("", response.bodyAsText())
            assertTrue(credentials.verifyCalls.isEmpty())
        }
    }

    @Test
    fun nothingIsWrittenWhenTheCredentialFails() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("correctpassword1" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"wrongpassword1"}""")
            }
            assertEquals(HttpStatusCode.Unauthorized, response.status)
            assertTrue(sessions.issued.isEmpty())
        }
    }

    @Test
    fun theResponseNeverEchoesWhatWasSent() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("correctpassword1" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            val response = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"correctpassword1"}""")
            }
            assertEquals(HttpStatusCode.OK, response.status)
            val body = response.bodyAsText()
            assertTrue(!body.contains("alice"))
            assertTrue(!body.contains("correctpassword1"))
        }
    }

    @Test
    fun signingOutAnswersTwoHundredAndFour() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("hunter2222" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            // First sign in to get a token
            val signInResponse = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.OK, signInResponse.status)
            val token = sessions.issued.single().second

            // Then sign out with that token
            val signOutResponse = client.post("/api/auth/sign-out") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
            }
            assertEquals(HttpStatusCode.NoContent, signOutResponse.status)
            assertEquals("", signOutResponse.bodyAsText())
            // Exactly one delete was recorded, and it names the right token
            assertEquals(1, sessions.deleted.size)
            assertEquals(token, sessions.deleted.single())
        }
    }

    @Test
    fun signingOutTwiceAnswersTwoHundredAndFourTwice() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("hunter2222" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            // First sign in to get a token
            val signInResponse = client.post("/api/auth/sign-in") {
                header(HttpHeaders.ContentType, "application/json")
                setBody("""{"handle":"alice","password":"hunter2222"}""")
            }
            assertEquals(HttpStatusCode.OK, signInResponse.status)
            val token = sessions.issued.single().second

            // Sign out
            val signOutResponse1 = client.post("/api/auth/sign-out") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
            }
            assertEquals(HttpStatusCode.NoContent, signOutResponse1.status)
            assertEquals("", signOutResponse1.bodyAsText())

            // Sign out again with the same token (idempotent)
            val signOutResponse2 = client.post("/api/auth/sign-out") {
                header(HttpHeaders.Authorization, "Bearer ${token.value}")
            }
            assertEquals(HttpStatusCode.NoContent, signOutResponse2.status)
            assertEquals("", signOutResponse2.bodyAsText())

            // Two deletes were recorded
            assertEquals(2, sessions.deleted.size)
            assertEquals(token, sessions.deleted[0])
            assertEquals(token, sessions.deleted[1])
        }
    }

    @Test
    fun signingOutWithNoHeaderAnswersTwoHundredAndFour() {
        testApplication {
            val credentials = SignInCredentials(emptyMap())
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            // Sign out with no Authorization header at all
            val response = client.post("/api/auth/sign-out")
            assertEquals(HttpStatusCode.NoContent, response.status)
            assertEquals("", response.bodyAsText())
            // No delete was recorded (delete was called zero times)
            assertEquals(0, sessions.deleted.size)
        }
    }

    @Test
    fun signingOutDeletesOnlyThePresentedToken() {
        testApplication {
            val credentials = SignInCredentials(mapOf("alice" to ("hunter2222" to PlayerId("p-alice"))))
            val sessions = RecordingAuthSessions()
            application {
                module()
                authRoutes(
                    FixedProfileReads(emptyMap()),
                    credentials,
                    identitiesFor(emptyMap()),
                    sessions,
                    freshSignUpBudget(),
                )
            }
            // Issue two tokens: first one for alice, second one for alice again
            sessions.issue(PlayerId("p-alice"))
            sessions.issue(PlayerId("p-alice"))
            val firstToken = sessions.issued[0].second
            val secondToken = sessions.issued[1].second

            // Sign out with the first token
            val response = client.post("/api/auth/sign-out") {
                header(HttpHeaders.Authorization, "Bearer ${firstToken.value}")
            }
            assertEquals(HttpStatusCode.NoContent, response.status)

            // Exactly one delete was recorded with the first token
            assertEquals(1, sessions.deleted.size)
            assertEquals(firstToken, sessions.deleted.single())

            // The second token is still resolvable through the double
            assertEquals(PlayerId("p-alice"), sessions.playerOf(secondToken))
        }
    }
}

/**
 * A fresh sign-up budget, never close to exhausted, for every test in this file that is not
 * itself about the budget — each sends at most one sign-up per address, so it never sees a `429`.
 */
private fun freshSignUpBudget(): AttemptBudget = AttemptBudget(AttemptLimits(5, 900_000L), MutableClock())

/**
 * The fields of a sign-in response worth comparing between two requests: status, body text and
 * the set of header names — never full header equality, because a header like `Date` is expected
 * to differ and asserting it would make this test flaky rather than meaningful.
 */
private data class SignInOutcome(val status: HttpStatusCode, val body: String, val headerNames: Set<String>)

/**
 * A credentials double for sign-in: [verify] answers from a fixture of correct identifier/password
 * pairs, exactly as [duels.poker.server.db.PostgresCredentials.verify] does — a wrong password and
 * an unknown identifier both answer `null`, indistinguishably, and every call is recorded so a test
 * can prove [verify] was never reached. [create] and [holdsCredential] are never called by sign-in
 * and throw if they ever are, the same idiom `RecordingCredentials.verify` used before this ticket
 * while only sign-up existed.
 */
private class SignInCredentials(private val correct: Map<String, Pair<String, PlayerId>>) : Credentials {
    val verifyCalls: MutableList<String> = mutableListOf()

    override suspend fun verify(kind: CredentialKind, identifier: String, presented: PresentedSecret): PlayerId? {
        verifyCalls.add(identifier)
        val (password, playerId) = correct[identifier] ?: return null
        return if (password == presented.value) playerId else null
    }

    override suspend fun create(
        playerId: PlayerId,
        kind: CredentialKind,
        identifier: String,
        secret: PresentedSecret,
    ): CreateCredentialResult {
        throw UnsupportedOperationException("sign-in never creates a credential")
    }

    override suspend fun holdsCredential(playerId: PlayerId, kind: CredentialKind): Boolean {
        throw UnsupportedOperationException("sign-in never checks holdsCredential")
    }
}

/**
 * An [AuthSessions] double for sign-in and sign-out: [issue] mints a token from nothing but a call
 * counter — never from [playerId] itself, so a token this double returns can never accidentally
 * echo a fixture's handle — and records the call, so a test can assert which player a token names
 * without comparing against any string this double repeats. [playerOf] answers from that same
 * recording, so a test can also confirm the token handed back in a response actually resolves to
 * the player who signed in. [delete] records every call, including calls with tokens that were
 * never issued.
 */
private class RecordingAuthSessions : AuthSessions {
    val issued: MutableList<Pair<PlayerId, SessionToken>> = mutableListOf()
    val deleted: MutableList<SessionToken> = mutableListOf()

    override suspend fun issue(playerId: PlayerId): SessionToken {
        val token = SessionToken("issued-session-token-${issued.size}")
        issued += playerId to token
        return token
    }

    override suspend fun playerOf(token: SessionToken): PlayerId? = issued.find { it.second == token }?.first

    override suspend fun delete(token: SessionToken) {
        deleted += token
    }
}
