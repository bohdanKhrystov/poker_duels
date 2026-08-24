package duels.poker.server.http

import duels.poker.server.auth.CreateCredentialResult
import duels.poker.server.auth.CredentialKind
import duels.poker.server.auth.PresentedSecret
import duels.poker.server.protocol.http.profileResponse
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class AuthRouteDoublesTest {
    @Test
    fun theRecorderStartsWithNothingRecorded() {
        val recorder = RecordingCredentials()
        assertTrue(recorder.createCalls.isEmpty())
        assertTrue(recorder.holdsCalls.isEmpty())
    }

    @Test
    fun theRecorderKeepsEveryArgumentItWasGiven() {
        val recorder = RecordingCredentials()
        val playerId = PlayerId("p-7")
        val kind = CredentialKind.PASSWORD
        val identifier = "bob"
        val secret = PresentedSecret("hunter2222")

        // Make the first call
        runBlocking {
            recorder.create(playerId, kind, identifier, secret)
        }

        assertEquals(1, recorder.createCalls.size)
        val call = recorder.createCalls.single()
        assertEquals(playerId, call.playerId)
        assertEquals(kind, call.kind)
        assertEquals(identifier, call.identifier)
        assertEquals(secret, call.secret)
    }

    @Test
    fun theRecorderKeepsArgumentsFromMultipleCalls() {
        val recorder = RecordingCredentials()
        val call1PlayerId = PlayerId("p-alice")
        val call1Kind = CredentialKind.PASSWORD
        val call1Identifier = "alice"
        val call1Secret = PresentedSecret("secret1")

        val call2PlayerId = PlayerId("p-bob")
        val call2Kind = CredentialKind("oauth")
        val call2Identifier = "bob@example.com"
        val call2Secret = PresentedSecret("secret2")

        runBlocking {
            recorder.create(call1PlayerId, call1Kind, call1Identifier, call1Secret)
            recorder.create(call2PlayerId, call2Kind, call2Identifier, call2Secret)
        }

        assertEquals(2, recorder.createCalls.size)

        val firstCall = recorder.createCalls[0]
        assertEquals(call1PlayerId, firstCall.playerId)
        assertEquals(call1Kind, firstCall.kind)
        assertEquals(call1Identifier, firstCall.identifier)
        assertEquals(call1Secret, firstCall.secret)

        val secondCall = recorder.createCalls[1]
        assertEquals(call2PlayerId, secondCall.playerId)
        assertEquals(call2Kind, secondCall.kind)
        assertEquals(call2Identifier, secondCall.identifier)
        assertEquals(call2Secret, secondCall.secret)
    }

    @Test
    fun theRecorderAnswersTheResultItWasBuiltWith() {
        val recorderCreated = RecordingCredentials(createResult = CreateCredentialResult.Created)
        val recorderTaken = RecordingCredentials(createResult = CreateCredentialResult.IdentifierTaken)

        val result1 = runBlocking {
            recorderCreated.create(
                PlayerId("p-1"),
                CredentialKind.PASSWORD,
                "user1",
                PresentedSecret("pass"),
            )
        }

        val result2 = runBlocking {
            recorderTaken.create(
                PlayerId("p-2"),
                CredentialKind.PASSWORD,
                "user2",
                PresentedSecret("pass"),
            )
        }

        assertEquals(CreateCredentialResult.Created, result1)
        assertEquals(CreateCredentialResult.IdentifierTaken, result2)
    }

    @Test
    fun theRecorderAnswersWhetherThePlayerHoldsOne() {
        val recorderTrue = RecordingCredentials(holds = true)
        val recorderFalse = RecordingCredentials(holds = false)

        val playerId = PlayerId("p-alice")
        val kind = CredentialKind.PASSWORD

        val result1 = runBlocking {
            recorderTrue.holdsCredential(playerId, kind)
        }

        val result2 = runBlocking {
            recorderFalse.holdsCredential(playerId, kind)
        }

        assertEquals(true, result1)
        assertEquals(false, result2)
    }

    @Test
    fun theRecorderRecordsHoldsCredentialCalls() {
        val recorder = RecordingCredentials()
        val playerId1 = PlayerId("p-alice")
        val kind1 = CredentialKind.PASSWORD

        val playerId2 = PlayerId("p-bob")
        val kind2 = CredentialKind("oauth")

        runBlocking {
            recorder.holdsCredential(playerId1, kind1)
            recorder.holdsCredential(playerId2, kind2)
        }

        assertEquals(2, recorder.holdsCalls.size)
        assertEquals(playerId1 to kind1, recorder.holdsCalls[0])
        assertEquals(playerId2 to kind2, recorder.holdsCalls[1])
    }

    @Test
    fun theRecorderVerifyThrows() {
        val recorder = RecordingCredentials()
        assertThrows(UnsupportedOperationException::class.java) {
            runBlocking {
                recorder.verify(
                    CredentialKind.PASSWORD,
                    "user",
                    PresentedSecret("pass"),
                )
            }
        }
    }

    @Test
    fun createCallsAndHoldsCallsAreNotConfused() {
        val recorder = RecordingCredentials()

        runBlocking {
            recorder.create(
                PlayerId("p-alice"),
                CredentialKind.PASSWORD,
                "alice",
                PresentedSecret("pass"),
            )
        }

        // After create, holdsCalls should still be empty
        assertTrue(recorder.holdsCalls.isEmpty())
        assertEquals(1, recorder.createCalls.size)

        runBlocking {
            recorder.holdsCredential(PlayerId("p-bob"), CredentialKind.PASSWORD)
        }

        // After holdsCredential, createCalls should not change
        assertEquals(1, recorder.createCalls.size)
        assertEquals(1, recorder.holdsCalls.size)
    }

    @Test
    fun theReadsDoubleAnswersOnlyForPlayerIdsItWasGiven() {
        val aliceProfile = profileResponse("p-alice", 100)
        val reads = FixedProfileReads(mapOf("alice" to aliceProfile))

        val aliceResult = runBlocking {
            reads.profileOf(PlayerId("p-alice"))
        }

        val malloryResult = runBlocking {
            reads.profileOf(PlayerId("p-mallory"))
        }

        assertEquals(aliceProfile, aliceResult)
        assertEquals(null, malloryResult)
        assertEquals(listOf("p-alice", "p-mallory"), reads.queried)
    }

    @Test
    fun theReadsDoubleRecordsAllQueriedPlayerIds() {
        val reads = FixedProfileReads(emptyMap())

        runBlocking {
            reads.profileOf(PlayerId("player-1"))
            reads.profileOf(PlayerId("player-2"))
            reads.profileOf(PlayerId("player-3"))
        }

        assertEquals(3, reads.queried.size)
        assertEquals(listOf("player-1", "player-2", "player-3"), reads.queried)
    }

    @Test
    fun theReadsDoubleReturnsEmptyDuelsList() {
        val reads = FixedProfileReads(emptyMap())

        val duels = runBlocking {
            reads.recentDuelsOf(PlayerId("p-alice"), 10)
        }

        assertTrue(duels.isEmpty())
    }
}
