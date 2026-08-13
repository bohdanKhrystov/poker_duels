package duels.poker.server.protocol.typescript

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/**
 * Extracts the type name from a descriptor's serialName.
 *
 * Takes the last dotted segment of `descriptor.serialName`, after removing a trailing `?`
 * (nullable marker). For example:
 * - `duels.poker.engine.game.PlayerView` → `PlayerView`
 * - `Act` → `Act` (sealed subtypes already have undotted serial names)
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun typeNameOf(descriptor: SerialDescriptor): String {
    val serialName = descriptor.serialName.removeSuffix("?")
    return serialName.substringAfterLast(".")
}

/**
 * Maps a descriptor to its TypeScript type reference.
 *
 * Given any [SerialDescriptor] the protocol reaches, returns the TypeScript type that should
 * appear on the wire. Dispatches on `descriptor.kind` exactly per ADR-0020:
 *
 * - STRING/CHAR primitives → `string`
 * - BOOLEAN primitive → `boolean`
 * - Numeric primitives (BYTE, SHORT, INT, LONG, FLOAT, DOUBLE) → `number`
 * - LIST (List, Set) → `readonly T[]` where T is the element descriptor's type
 * - CLASS, OBJECT, ENUM, SEALED → the type name from [typeNameOf]
 * - Anything else → throws [IllegalArgumentException] with kind and serial name
 *
 * If [descriptor.isNullable] is true, the result is wrapped as `T | null`.
 *
 * The dispatch is on the descriptor kind, never the serial name or Kotlin class shape. This is
 * crucial for custom serializers: [Card] is a `@JvmInline value class` over an [Int], but its
 * [CardSerializer] declares a STRING descriptor, so it emits as `string` on the wire.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun typeReference(descriptor: SerialDescriptor): String {
    val base = when (descriptor.kind) {
        PrimitiveKind.STRING, PrimitiveKind.CHAR -> "string"
        PrimitiveKind.BOOLEAN -> "boolean"
        PrimitiveKind.BYTE, PrimitiveKind.SHORT, PrimitiveKind.INT,
        PrimitiveKind.LONG, PrimitiveKind.FLOAT, PrimitiveKind.DOUBLE,
        -> "number"
        StructureKind.LIST -> {
            val elementType = typeReference(descriptor.getElementDescriptor(0))
            "readonly $elementType[]"
        }
        StructureKind.CLASS, StructureKind.OBJECT, SerialKind.ENUM,
        PolymorphicKind.SEALED,
        -> typeNameOf(descriptor)
        else -> throw IllegalArgumentException(
            "Unsupported descriptor kind: ${descriptor.kind} (serialName: ${descriptor.serialName})",
        )
    }

    return if (descriptor.isNullable) "$base | null" else base
}
