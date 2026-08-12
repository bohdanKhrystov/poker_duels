package duels.poker.server.db

import org.junit.jupiter.api.Assumptions
import org.postgresql.ds.PGSimpleDataSource
import org.testcontainers.DockerClientFactory
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.utility.DockerImageName
import javax.sql.DataSource

/**
 * One PostgreSQL container for the whole test suite, gated by whether Docker is available and
 * whether it is required.
 *
 * A machine without Docker skips database tests with a message naming the remedy, so the engine
 * and protocol stay testable without it. CI sets `-PrequireDocker=true` (or `CI`), so a runner
 * without Docker fails the build instead of quietly skipping — a suite that skips silently in CI
 * is a suite that has stopped testing.
 */
public object PostgresTestSupport {
    public const val IMAGE: String = "postgres:16-alpine"

    internal enum class Policy { RUN, SKIP, FAIL }

    /**
     * The whole decision, as a pure function so it can be tested on any machine, Docker or not.
     */
    internal fun policy(dockerAvailable: Boolean, dockerRequired: Boolean): Policy =
        when {
            dockerAvailable -> Policy.RUN
            dockerRequired -> Policy.FAIL
            else -> Policy.SKIP
        }

    // Started on first use, never stopped explicitly — Testcontainers' Ryuk removes it when the
    // JVM exits. One container for the whole suite, not one per test.
    private val container: PostgreSQLContainer<*> by lazy {
        PostgreSQLContainer(DockerImageName.parse(IMAGE)).also { it.start() }
    }

    private fun dockerRequired(): Boolean =
        System.getProperty("poker.requireDocker") == "true" || System.getenv("CI") != null

    /**
     * Gates any test that needs a database. Call as the first statement of any test that needs
     * one, so the gate applies automatically.
     */
    public fun requireDocker() {
        val dockerAvailable = DockerClientFactory.instance().isDockerAvailable
        when (policy(dockerAvailable, dockerRequired())) {
            Policy.RUN -> return
            Policy.SKIP ->
                Assumptions.assumeTrue(
                    false,
                    "Docker is not available, so the PostgreSQL tests were skipped. Start Docker, " +
                        "or run with -PrequireDocker=true to make this a failure.",
                )
            Policy.FAIL ->
                throw IllegalStateException(
                    "Docker was required (poker.requireDocker=true or CI is set) but is not available.",
                )
        }
    }

    /**
     * A fresh, unpooled connection to an empty `public` schema on the shared container. Dropping
     * the schema also drops Flyway's history table, so "apply every migration to an empty
     * database" means what it says.
     */
    public fun freshDatabase(): DataSource {
        requireDocker()
        val source =
            PGSimpleDataSource().apply {
                setUrl(container.jdbcUrl)
                user = container.username
                password = container.password
            }
        source.connection.use { connection ->
            connection.createStatement().use { it.execute("DROP SCHEMA public CASCADE; CREATE SCHEMA public") }
        }
        return source
    }
}
