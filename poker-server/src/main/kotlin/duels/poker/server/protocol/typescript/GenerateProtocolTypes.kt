package duels.poker.server.protocol.typescript

import java.io.File

/**
 * Entry point for the `generateProtocolTypes` Gradle task (ADR-0020).
 *
 * Writes [protocolTypeScript] verbatim to the single path given in [args] — the committed
 * `web-client/src/protocol/protocol.gen.ts`. Public because Gradle's `JavaExec` launches it by
 * class name; not meant to run any other way.
 */
public fun main(args: Array<String>) {
    require(args.size == 1) {
        "generateProtocolTypes requires exactly one argument: the target file path"
    }
    val target = File(args[0])
    target.parentFile?.mkdirs()
    target.writeText(protocolTypeScript())
}
