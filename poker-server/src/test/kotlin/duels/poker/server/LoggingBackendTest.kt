package duels.poker.server

import org.junit.jupiter.api.Test
import org.slf4j.LoggerFactory
import org.slf4j.helpers.NOPLoggerFactory
import kotlin.test.assertFalse

class LoggingBackendTest {
    @Test
    fun `a logger factory binds a real implementation`() {
        // Verify that a real logging backend is bound, not SLF4J's no-op fallback.
        // The NOPLoggerFactory discards all log messages, making "log and continue"
        // (ADR-0025) become "swallow and continue". This test proves we've bound a real
        // backend that will actually record errors.
        val factory = LoggerFactory.getILoggerFactory()
        assertFalse(
            factory is NOPLoggerFactory,
            "SLF4J factory should not be NOPLoggerFactory; a real backend should be bound",
        )
    }
}
