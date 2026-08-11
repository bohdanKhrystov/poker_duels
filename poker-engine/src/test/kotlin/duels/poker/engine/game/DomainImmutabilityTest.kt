package duels.poker.engine.game

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.reflect.Modifier

class DomainImmutabilityTest {
    // Every value type in duels.poker.engine.game. Adding a domain type means adding it here;
    // the JVM erases Kotlin's read-only/mutable list distinction, so this list is the only
    // handle a test has on the package.
    private val domainTypes: List<Class<*>> = listOf(
        GameState::class.java, Seat::class.java, Board::class.java, LegalActions::class.java,
        EngineResult::class.java,
        HandStarted::class.java, BlindPosted::class.java, HoleCardsDealt::class.java,
        ActionOn::class.java,
        PlayerFolded::class.java, PlayerChecked::class.java, PlayerCalled::class.java,
        PlayerBet::class.java, PlayerRaised::class.java, PlayerAllIn::class.java,
        BettingRoundEnded::class.java, StreetDealt::class.java, ShowdownReached::class.java,
        HandRevealed::class.java, UncalledBetReturned::class.java, PotAwarded::class.java,
        HandFinished::class.java,
        PlayerAction.Fold::class.java, PlayerAction.Check::class.java,
        PlayerAction.Call::class.java, PlayerAction.Bet::class.java,
        PlayerAction.Raise::class.java, PlayerAction.AllIn::class.java,
        Rejection.NotYourTurn::class.java, Rejection.ActionNotAllowed::class.java,
        Rejection.AmountTooSmall::class.java, Rejection.AmountTooLarge::class.java,
    )

    @Test
    fun noDomainTypeHasANonFinalField() {
        val violations = mutableListOf<String>()
        for (clazz in domainTypes) {
            for (field in clazz.declaredFields) {
                // Skip synthetic and static fields
                if (field.isSynthetic || Modifier.isStatic(field.modifiers)) {
                    continue
                }
                // Check if field is final
                if (!Modifier.isFinal(field.modifiers)) {
                    violations.add("${clazz.simpleName}.${field.name} is not final (mutable)")
                }
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun noDomainTypeExposesASetter() {
        val violations = mutableListOf<String>()
        for (clazz in domainTypes) {
            for (method in clazz.declaredMethods) {
                // Skip synthetic methods
                if (method.isSynthetic) {
                    continue
                }
                // Check for setter pattern: public method starting with "set" and exactly one parameter
                if (method.name.startsWith("set") && method.parameterCount == 1) {
                    if (Modifier.isPublic(method.modifiers)) {
                        violations.add("${clazz.simpleName}.${method.name}() is a public setter")
                    }
                }
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun everyDomainTypeDefinesValueEquality() {
        val violations = mutableListOf<String>()
        for (clazz in domainTypes) {
            // Check if the class declares its own equals method (not inherited from Any)
            val hasOwnEquals = clazz.declaredMethods.any { it.name == "equals" && it.parameterCount == 1 }
            // Check if the class declares its own hashCode method (not inherited from Any)
            val hasOwnHashCode = clazz.declaredMethods.any { it.name == "hashCode" && it.parameterCount == 0 }

            if (!hasOwnEquals) {
                violations.add("${clazz.simpleName} does not define its own equals method")
            }
            if (!hasOwnHashCode) {
                violations.add("${clazz.simpleName} does not define its own hashCode method")
            }
        }
        assertTrue(violations.isEmpty(), violations.joinToString("\n"))
    }

    @Test
    fun theDomainTypeListIsNotEmpty() {
        assertEquals(32, domainTypes.size, "Domain type list should have at least 30 types to prevent vacuous green")
    }
}
