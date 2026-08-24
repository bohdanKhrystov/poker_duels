package duels.poker.server.db

import duels.poker.server.auth.SessionToken
import org.junit.jupiter.api.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertFalse

class SessionTokenDigestTest {
    @Test
    fun theDigestOfAKnownTokenIsTheKnownSha256() {
        val digest = sessionTokenDigest(SessionToken("abc"))
        val expected = "ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad"
            .chunked(2)
            .map { it.toInt(16).toByte() }
            .toByteArray()
        assertContentEquals(expected, digest)
    }

    @Test
    fun twoDifferentTokensDigestDifferently() {
        val digestA = sessionTokenDigest(SessionToken("a"))
        val digestB = sessionTokenDigest(SessionToken("b"))
        assertFalse(digestA.contentEquals(digestB))
    }

    @Test
    fun theSameTokenDigestsIdentically() {
        val token = SessionToken("a")
        val digest1 = sessionTokenDigest(token)
        val digest2 = sessionTokenDigest(token)
        assertContentEquals(digest1, digest2)
    }
}
