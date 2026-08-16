package duels.poker.server.duel

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

/**
 * Entry point for the `generateDuelScript` Gradle task (`TASK-031203`).
 *
 * Writes [scriptedDuel] verbatim, JSON-encoded, to the single path given in [args] — the
 * committed `web-client/src/e2e/scripted-duel.gen.json`. Public because Gradle's `JavaExec`
 * launches it by class name; not meant to run any other way. Its own [Json] rather than
 * `protocolJson`, because a [ScriptedDuel] is a fixture and not a wire message. No trailing
 * newline: byte-for-byte is byte-for-byte.
 */
public fun main(args: Array<String>) {
    require(args.size == 1) {
        "generateDuelScript requires exactly one argument: the target file path"
    }
    val target = File(args[0])
    target.parentFile?.mkdirs()
    target.writeText(Json { prettyPrint = false }.encodeToString(scriptedDuel()))
}
