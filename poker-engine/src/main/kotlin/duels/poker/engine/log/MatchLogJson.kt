package duels.poker.engine.log

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val matchLogJson = Json { encodeDefaults = true }

public fun encodeMatchLog(log: MatchLog): String {
    return matchLogJson.encodeToString(MatchLog.serializer(), log)
}

public fun decodeMatchLog(text: String): MatchLog {
    // Parse to JsonElement first to check version before decoding
    val element = try {
        matchLogJson.parseToJsonElement(text)
    } catch (e: Exception) {
        throw IllegalArgumentException("Failed to parse match log JSON: ${e.message}", e)
    }

    // Extract and validate version from the parsed JsonElement
    val versionElement = try {
        element.jsonObject["version"]
            ?: throw IllegalArgumentException("Match log is missing required 'version' member")
    } catch (e: IllegalArgumentException) {
        throw e
    } catch (e: Exception) {
        throw IllegalArgumentException("Match log 'version' member is not accessible: ${e.message}", e)
    }

    val version = try {
        versionElement.jsonPrimitive.content.toInt()
    } catch (e: Exception) {
        throw IllegalArgumentException("Match log 'version' member is not an integer: ${e.message}", e)
    }

    if (version != MATCH_LOG_VERSION) {
        throw IllegalArgumentException(
            "Match log version mismatch: expected $MATCH_LOG_VERSION, got $version",
        )
    }

    // Decode the MatchLog from the JsonElement
    return matchLogJson.decodeFromJsonElement(MatchLog.serializer(), element)
}
