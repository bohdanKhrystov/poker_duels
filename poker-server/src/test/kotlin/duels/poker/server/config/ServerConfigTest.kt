package duels.poker.server.config

import duels.poker.server.auth.AttemptLimits
import duels.poker.server.room.RoomTimeouts
import io.ktor.server.config.MapApplicationConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class ServerConfigTest {
    @Test
    fun readsThePortFromTheConfig() {
        val config = MapApplicationConfig("server.port" to "7000")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(7000, serverConfig.port)
    }

    @Test
    fun theEnvironmentVariableOverridesTheConfig() {
        val config = MapApplicationConfig("server.port" to "7000")
        val serverConfig = ServerConfig.from(config) { name -> if (name == ServerConfig.PORT_ENV) "9001" else null }
        assertEquals(9001, serverConfig.port)
    }

    @Test
    fun fallsBackToTheDefaultWhenNothingIsSet() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(ServerConfig.DEFAULT_PORT, serverConfig.port)
    }

    @Test
    fun rejectsAPortThatIsNotANumber() {
        val config = MapApplicationConfig("server.port" to "eighty-eighty")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun loadsThePortFromTheShippedApplicationConf() {
        val serverConfig = ServerConfig.load { null }
        assertEquals(8080, serverConfig.port)
    }

    @Test
    fun theEnvironmentVariableOverridesTheShippedFile() {
        val serverConfig = ServerConfig.load { name -> if (name == ServerConfig.PORT_ENV) "9001" else null }
        assertEquals(9001, serverConfig.port)
    }

    @Test
    fun readsTheDatabaseUrlFromTheConfig() {
        val config = MapApplicationConfig("database.url" to "jdbc:postgresql://db:5432/x")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals("jdbc:postgresql://db:5432/x", serverConfig.databaseUrl)
    }

    @Test
    fun theEnvironmentVariableOverridesTheDatabaseUrl() {
        val config = MapApplicationConfig("database.url" to "jdbc:postgresql://db:5432/x")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.DATABASE_URL_ENV) "jdbc:postgresql://env:5432/y" else null
        }
        assertEquals("jdbc:postgresql://env:5432/y", serverConfig.databaseUrl)
    }

    @Test
    fun fallsBackToTheDefaultDatabaseSettings() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(ServerConfig.DEFAULT_DATABASE_URL, serverConfig.databaseUrl)
        assertEquals(ServerConfig.DEFAULT_DATABASE_USER, serverConfig.databaseUser)
        assertEquals(ServerConfig.DEFAULT_DATABASE_PASSWORD, serverConfig.databasePassword)
        assertEquals(ServerConfig.DEFAULT_DATABASE_POOL_SIZE, serverConfig.databasePoolSize)
    }

    @Test
    fun rejectsAPoolSizeThatIsNotANumber() {
        val config = MapApplicationConfig("database.poolSize" to "many")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun loadsTheDatabaseSettingsFromTheShippedApplicationConf() {
        val serverConfig = ServerConfig.load { null }
        assertEquals(ServerConfig.DEFAULT_DATABASE_URL, serverConfig.databaseUrl)
        assertEquals(ServerConfig.DEFAULT_DATABASE_USER, serverConfig.databaseUser)
        assertEquals(ServerConfig.DEFAULT_DATABASE_PASSWORD, serverConfig.databasePassword)
        assertEquals(ServerConfig.DEFAULT_DATABASE_POOL_SIZE, serverConfig.databasePoolSize)
    }

    @Test
    fun readsTheRoomWaitingTimeoutFromTheConfig() {
        val config = MapApplicationConfig("room.waitingTimeoutMillis" to "1234")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(1234L, serverConfig.roomWaitingTimeoutMillis)
    }

    @Test
    fun theEnvironmentVariableOverridesTheRoomFinishedTimeout() {
        val config = MapApplicationConfig("room.finishedTimeoutMillis" to "1234")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.ROOM_FINISHED_TIMEOUT_MILLIS_ENV) "5678" else null
        }
        assertEquals(5678L, serverConfig.roomFinishedTimeoutMillis)
    }

    @Test
    fun fallsBackToTheRoomTimeoutDefaults() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(RoomTimeouts.DEFAULT_WAITING_MILLIS, serverConfig.roomWaitingTimeoutMillis)
        assertEquals(RoomTimeouts.DEFAULT_FINISHED_MILLIS, serverConfig.roomFinishedTimeoutMillis)
    }

    @Test
    fun rejectsARoomTimeoutThatIsNotANumber() {
        val config = MapApplicationConfig("room.waitingTimeoutMillis" to "ten minutes")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun rejectsANonPositiveRoomTimeout() {
        val config = MapApplicationConfig("room.finishedTimeoutMillis" to "0")
        val serverConfig = ServerConfig.from(config) { null }
        assertThrows<IllegalArgumentException> {
            serverConfig.roomTimeouts()
        }
    }

    @Test
    fun roomTimeoutsBundlesAllThreeValues() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(
            RoomTimeouts(serverConfig.roomWaitingTimeoutMillis, serverConfig.roomFinishedTimeoutMillis, serverConfig.disconnectGraceMillis),
            serverConfig.roomTimeouts(),
        )
    }

    @Test
    fun readsTheGraceWindowFromTheConfig() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "45000")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(45_000L, serverConfig.disconnectGraceMillis)
    }

    @Test
    fun theEnvironmentVariableOverridesTheGraceWindow() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "45000")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.DISCONNECT_GRACE_MILLIS_ENV) "90000" else null
        }
        assertEquals(90_000L, serverConfig.disconnectGraceMillis)
    }

    @Test
    fun rejectsAGraceWindowThatIsNotANumber() {
        val config = MapApplicationConfig("duel.disconnectGraceMillis" to "a minute")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun readsTheSweepPeriodFromTheConfig() {
        val config = MapApplicationConfig("server.sweepPeriodMillis" to "2500")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(2_500L, serverConfig.sweepPeriodMillis)
    }

    @Test
    fun theEnvironmentVariableOverridesTheSweepPeriod() {
        val config = MapApplicationConfig("server.sweepPeriodMillis" to "2500")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.SWEEP_PERIOD_MILLIS_ENV) "5000" else null
        }
        assertEquals(5_000L, serverConfig.sweepPeriodMillis)
    }

    @Test
    fun rejectsANonNumericSweepPeriod() {
        val config = MapApplicationConfig("server.sweepPeriodMillis" to "not a number")
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun theDefaultSweepPeriodIsOneThousand() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(1_000L, serverConfig.sweepPeriodMillis)
    }

    @Test
    fun theSignUpBudgetDefaults() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(5, serverConfig.signUpMaxAttempts)
        assertEquals(900_000L, serverConfig.signUpWindowMillis)
    }

    @Test
    fun theSignUpBudgetComesFromTheEnvironment() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { name ->
            when (name) {
                ServerConfig.AUTH_SIGN_UP_MAX_ATTEMPTS -> "9"
                ServerConfig.AUTH_SIGN_UP_WINDOW_MILLIS -> "1000"
                else -> null
            }
        }
        assertEquals(9, serverConfig.signUpMaxAttempts)
        assertEquals(1000L, serverConfig.signUpWindowMillis)
    }

    @Test
    fun theSignUpBudgetComesFromTheConfigFile() {
        val config = MapApplicationConfig(
            ServerConfig.SIGN_UP_MAX_ATTEMPTS_KEY to "8",
            ServerConfig.SIGN_UP_WINDOW_MILLIS_KEY to "2000",
        )
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(8, serverConfig.signUpMaxAttempts)
        assertEquals(2000L, serverConfig.signUpWindowMillis)
    }

    @Test
    fun theSignInBudgetDefaults() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(10, serverConfig.signInMaxAttempts)
        assertEquals(60_000L, serverConfig.signInWindowMillis)
    }

    @Test
    fun theSignInBudgetComesFromTheEnvironment() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { name ->
            when (name) {
                ServerConfig.AUTH_SIGN_IN_MAX_ATTEMPTS -> "7"
                ServerConfig.AUTH_SIGN_IN_WINDOW_MILLIS -> "1234"
                else -> null
            }
        }
        assertEquals(7, serverConfig.signInMaxAttempts)
        assertEquals(1234L, serverConfig.signInWindowMillis)
    }

    @Test
    fun theSignInBudgetComesFromTheConfigFile() {
        val config = MapApplicationConfig(
            ServerConfig.SIGN_IN_MAX_ATTEMPTS_KEY to "6",
            ServerConfig.SIGN_IN_WINDOW_MILLIS_KEY to "5678",
        )
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(6, serverConfig.signInMaxAttempts)
        assertEquals(5678L, serverConfig.signInWindowMillis)
    }

    @Test
    fun theTwoBudgetsAreSeparateValues() {
        val config1 = MapApplicationConfig()
        val serverConfig1 = ServerConfig.from(config1) { name ->
            if (name == ServerConfig.AUTH_SIGN_UP_MAX_ATTEMPTS) "12" else null
        }
        assertEquals(12, serverConfig1.signUpMaxAttempts)
        assertEquals(10, serverConfig1.signInMaxAttempts)

        val config2 = MapApplicationConfig()
        val serverConfig2 = ServerConfig.from(config2) { name ->
            if (name == ServerConfig.AUTH_SIGN_IN_WINDOW_MILLIS) "3000" else null
        }
        assertEquals(900_000L, serverConfig2.signUpWindowMillis)
        assertEquals(3000L, serverConfig2.signInWindowMillis)
    }

    @Test
    fun signUpLimitsCarriesBothNumbers() {
        val config = MapApplicationConfig(
            ServerConfig.SIGN_UP_MAX_ATTEMPTS_KEY to "3",
            ServerConfig.SIGN_UP_WINDOW_MILLIS_KEY to "500000",
        )
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(AttemptLimits(3, 500_000L), serverConfig.signUpLimits())
    }

    @Test
    fun aNonNumericBudgetIsRefused() {
        val config = MapApplicationConfig()
        assertThrows<IllegalArgumentException> {
            ServerConfig.from(config) { name ->
                if (name == ServerConfig.AUTH_SIGN_UP_MAX_ATTEMPTS) "many" else null
            }
        }
    }

    @Test
    fun readsTheBaseUrlFromTheConfig() {
        val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "https://example.com")
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals("https://example.com", serverConfig.baseUrl)
    }

    @Test
    fun theEnvironmentVariableOverridesTheBaseUrl() {
        val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "https://config.test")
        val serverConfig = ServerConfig.from(config) { name ->
            if (name == ServerConfig.BASE_URL_ENV) "https://env.test" else null
        }
        assertEquals("https://env.test", serverConfig.baseUrl)
    }

    @Test
    fun fallsBackToTheDefaultBaseUrl() {
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals("http://localhost:5173", serverConfig.baseUrl)
    }

    @Test
    fun rejectsABaseUrlThatIsNotAnOrigin() {
        assertThrows<IllegalArgumentException> {
            val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "localhost:5173")
            ServerConfig.from(config) { null }
        }

        assertThrows<IllegalArgumentException> {
            val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "ftp://x.test")
            ServerConfig.from(config) { null }
        }

        assertThrows<IllegalArgumentException> {
            val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "http://x.test/")
            ServerConfig.from(config) { null }
        }

        assertThrows<IllegalArgumentException> {
            val config = MapApplicationConfig(ServerConfig.BASE_URL_KEY to "")
            ServerConfig.from(config) { null }
        }
    }

    @Test
    fun forgotPasswordIsTenAMinuteWithNothingConfigured() {
        // ADR-0079: generous at 10 / 60000 because fifteen-minute per-account rule already caps bombing
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(AttemptLimits(10, 60_000L), serverConfig.forgotPasswordLimits())
    }

    @Test
    fun recoveryEmailIsFiveAMinuteWithNothingConfigured() {
        // ADR-0079: 5 / 60000 because it is the only cap on mail to caller-chosen recipient
        val config = MapApplicationConfig()
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(AttemptLimits(5, 60_000L), serverConfig.recoveryEmailLimits())
    }

    @Test
    fun eachRecoveryBudgetReadsItsOwnKeys() {
        // ADR-0079: both recovery budgets must read their own keys, not cross-wired
        val config = MapApplicationConfig(
            ServerConfig.FORGOT_PASSWORD_MAX_ATTEMPTS_KEY to "3",
            ServerConfig.FORGOT_PASSWORD_WINDOW_MILLIS_KEY to "111000",
            ServerConfig.RECOVERY_EMAIL_MAX_ATTEMPTS_KEY to "7",
            ServerConfig.RECOVERY_EMAIL_WINDOW_MILLIS_KEY to "222000",
        )
        val serverConfig = ServerConfig.from(config) { null }
        assertEquals(AttemptLimits(3, 111_000L), serverConfig.forgotPasswordLimits())
        assertEquals(AttemptLimits(7, 222_000L), serverConfig.recoveryEmailLimits())
    }

    @Test
    fun theEnvironmentOverridesBothRecoveryBudgets() {
        // ADR-0079: environment must override both recovery budgets from file
        val config = MapApplicationConfig(
            ServerConfig.FORGOT_PASSWORD_MAX_ATTEMPTS_KEY to "3",
            ServerConfig.FORGOT_PASSWORD_WINDOW_MILLIS_KEY to "111000",
            ServerConfig.RECOVERY_EMAIL_MAX_ATTEMPTS_KEY to "7",
            ServerConfig.RECOVERY_EMAIL_WINDOW_MILLIS_KEY to "222000",
        )
        val serverConfig = ServerConfig.from(config) { name ->
            when (name) {
                ServerConfig.AUTH_FORGOT_PASSWORD_MAX_ATTEMPTS -> "2"
                ServerConfig.AUTH_FORGOT_PASSWORD_WINDOW_MILLIS -> "333000"
                ServerConfig.AUTH_RECOVERY_EMAIL_MAX_ATTEMPTS -> "4"
                ServerConfig.AUTH_RECOVERY_EMAIL_WINDOW_MILLIS -> "444000"
                else -> null
            }
        }
        assertEquals(AttemptLimits(2, 333_000L), serverConfig.forgotPasswordLimits())
        assertEquals(AttemptLimits(4, 444_000L), serverConfig.recoveryEmailLimits())
    }
}
