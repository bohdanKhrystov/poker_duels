package duels.poker.server.http

import duels.poker.server.protocol.http.ProfileResponse
import duels.poker.server.session.PlayerId
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import kotlin.reflect.full.memberFunctions
import kotlin.reflect.full.memberProperties
import kotlin.test.assertContains
import kotlin.test.assertIs
import kotlin.test.fail

class ProfileWritesPortTest {
    @Test
    fun noPortFunctionTakesANameAndReturnsAnIdentity() {
        val matchingMembers = mutableListOf<String>()

        // Check ProfileReads
        for (func in ProfileReads::class.memberFunctions) {
            if (hasNameParameterAndReturnsIdentity(func)) {
                matchingMembers.add(func.name)
            }
        }
        for (prop in ProfileReads::class.memberProperties) {
            if (hasNameParameterAndReturnsIdentity(prop)) {
                matchingMembers.add(prop.name)
            }
        }

        // Check ProfileWrites
        for (func in ProfileWrites::class.memberFunctions) {
            if (hasNameParameterAndReturnsIdentity(func)) {
                matchingMembers.add(func.name)
            }
        }
        for (prop in ProfileWrites::class.memberProperties) {
            if (hasNameParameterAndReturnsIdentity(prop)) {
                matchingMembers.add(prop.name)
            }
        }

        if (matchingMembers.isNotEmpty()) {
            fail(
                "Found ${matchingMembers.size} function(s) that take a display name and " +
                    "return an identity type: $matchingMembers",
            )
        }
    }

    private fun hasNameParameterAndReturnsIdentity(member: kotlin.reflect.KCallable<*>): Boolean {
        var hasNameParameter = false

        for (param in member.parameters) {
            if (param.type.classifier == String::class) {
                val paramName = param.name
                if (
                    paramName != null && (
                        paramName.contains("name", ignoreCase = true) ||
                            paramName.contains("displayName", ignoreCase = true) ||
                            paramName.contains("canonicalName", ignoreCase = true)
                        )
                ) {
                    hasNameParameter = true
                    break
                }
            }
        }

        if (!hasNameParameter) {
            return false
        }

        val returnTypeClassName = member.returnType.classifier?.let {
            (it as? Class<*>)?.simpleName ?: it.toString()
        } ?: ""

        return returnTypeClassName == "PlayerId" ||
            returnTypeClassName == "DeviceId" ||
            returnTypeClassName == "ProfileResponse"
    }

    @Test
    fun theResultIsSealedAndHasExactlyThreeCases() {
        val sealedSubclasses = SetNameResult::class.sealedSubclasses

        assertEquals(3, sealedSubclasses.size, "SetNameResult must have exactly 3 sealed subclasses")

        val classNames = sealedSubclasses.map { it.simpleName }.toSet()
        assertContains(
            classNames,
            "NameSet",
            "SetNameResult must have a NameSet sealed subclass",
        )
        assertContains(
            classNames,
            "NameTaken",
            "SetNameResult must have a NameTaken sealed subclass",
        )
        assertContains(
            classNames,
            "AlreadyNamed",
            "SetNameResult must have an AlreadyNamed sealed subclass",
        )
    }

    @Test
    fun settingANameReturnsTheProfileItProduced() {
        val testProfile = ProfileResponse(
            playerId = "1",
            coinBalance = 42,
            displayName = "TestName",
        )

        val testDouble = object : ProfileWrites {
            override suspend fun setDisplayName(
                playerId: PlayerId,
                canonicalName: String,
            ): SetNameResult = SetNameResult.NameSet(testProfile)
        }

        val result = runBlocking {
            testDouble.setDisplayName(PlayerId("1"), "TestName")
        }
        assertIs<SetNameResult.NameSet>(result)
        assertEquals(testProfile, result.profile)
    }
}
