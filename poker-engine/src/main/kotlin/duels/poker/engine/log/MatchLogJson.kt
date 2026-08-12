package duels.poker.engine.log

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
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

    // A current outer version says nothing about the hands nested inside it: each HandLog
    // carries its own version, and a match log written by this build can still embed a hand
    // written by a future one. Walk the parsed "hands" array before decoding, the same way the
    // outer version above and decodeHandLog's own guard both work, rather than routing each hand
    // through decodeHandLog — that would re-serialize and re-parse every hand a second time. This
    // keeps the whole document to the one parse already done above.
    val handsElement = element.jsonObject["hands"]
    if (handsElement is JsonArray) {
        handsElement.forEachIndexed { index, hand ->
            val handNumber = index + 1

            val handVersionElement = hand.jsonObject["version"]
                ?: throw IllegalArgumentException(
                    "Hand $handNumber is missing required 'version' member",
                )

            val handVersion = try {
                handVersionElement.jsonPrimitive.content.toInt()
            } catch (e: Exception) {
                throw IllegalArgumentException(
                    "Hand $handNumber 'version' member is not an integer: ${e.message}",
                    e,
                )
            }

            if (handVersion != HAND_LOG_VERSION) {
                throw IllegalArgumentException(
                    "Hand $handNumber version mismatch: expected $HAND_LOG_VERSION, got $handVersion",
                )
            }
        }
    }

    // Decode the MatchLog from the JsonElement
    return matchLogJson.decodeFromJsonElement(MatchLog.serializer(), element)
}
