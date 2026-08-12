package duels.poker.engine.log

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.decodeFromJsonElement

/** The shared Json instance for encoding and decoding hand logs. */
private val handLogJson = Json { encodeDefaults = true }

/**
 * Encode a [HandLog] to a JSON string, ensuring the [version] field is always written.
 *
 * @return the JSON string representation of [log]
 */
public fun encodeHandLog(log: HandLog): String = handLogJson.encodeToString(HandLog.serializer(), log)

/**
 * Decode a hand log from a JSON string, verifying the version before attempting to decode.
 *
 * Parses the text to a [JsonElement] first to read the version from the JSON object itself,
 * not from the decoded [HandLog] where a default would have filled in the missing version.
 * This way, a log with an unknown or missing version is caught before decoding begins.
 *
 * @param text the JSON string to decode
 * @return the decoded [HandLog]
 * @throws IllegalArgumentException if the text is not valid JSON, is not a JSON object,
 * contains no `version` member, or contains a version other than [HAND_LOG_VERSION]
 */
public fun decodeHandLog(text: String): HandLog {
    // Parse to JsonElement first to inspect the version before decoding
    val element = try {
        handLogJson.parseToJsonElement(text)
    } catch (e: Exception) {
        throw IllegalArgumentException("Invalid JSON: ${e.message}", e)
    }

    // Verify it's a JSON object
    if (element !is JsonObject) {
        throw IllegalArgumentException("Hand log must be a JSON object, not ${element::class.simpleName}")
    }

    // Check that version member exists and matches expected version
    val versionElement = element["version"]
        ?: throw IllegalArgumentException("Hand log missing required 'version' member")

    val version = (versionElement as? JsonPrimitive)?.content?.toIntOrNull()
        ?: throw IllegalArgumentException("'version' member is not a valid integer")

    if (version != HAND_LOG_VERSION) {
        throw IllegalArgumentException("Hand log version $version does not match expected version $HAND_LOG_VERSION")
    }

    // Now safe to decode
    return handLogJson.decodeFromJsonElement(HandLog.serializer(), element)
}
