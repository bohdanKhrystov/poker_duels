package duels.poker.server.protocol.typescript

import duels.poker.server.protocol.ClientMessage
import duels.poker.server.protocol.ServerMessage
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PolymorphicKind
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.SerialKind
import kotlinx.serialization.descriptors.StructureKind

/**
 * One named TypeScript declaration discovered while walking the protocol's descriptors.
 *
 * @property name The declaration's TypeScript type name, from [typeNameOf].
 * @property text The full declaration — an `export type` or `export interface` statement.
 */
internal data class TypeScriptDeclaration(val name: String, val text: String)

/** The protocol's two message roots, walked by [protocolDeclarations] in this order. */
@OptIn(ExperimentalSerializationApi::class)
internal val protocolRoots: List<SerialDescriptor> = listOf(
    ClientMessage.serializer().descriptor,
    ServerMessage.serializer().descriptor,
)

/**
 * Walks [roots] depth-first and returns every TypeScript declaration the protocol needs, each
 * exactly once, in discovery order.
 *
 * Dispatch is on `descriptor.kind`, per the table in ADR-0020:
 * - **SEALED** emits [unionDeclaration], then recurses into each [sealedVariants] subtype,
 *   passing its serial name down as that subtype's discriminator.
 * - **CLASS** / **OBJECT** emits [interfaceDeclaration] with the discriminator carried from the
 *   caller, then recurses into every element descriptor with a `null` discriminator.
 * - **ENUM** emits [enumDeclaration] and recurses into nothing: an enum descriptor's own element
 *   descriptors are its entries, not types to declare.
 * - **LIST** (`List`, `Set`) emits nothing and recurses once, into its element descriptor.
 * - A **PRIMITIVE** emits nothing and recurses into nothing.
 * - Any other kind (MAP, CONTEXTUAL, open polymorphism, ...) throws [IllegalArgumentException]
 *   naming the kind and the serial name — the closed-surface guarantee ADR-0020 asks for.
 *
 * Only the named kinds — SEALED, CLASS, OBJECT, ENUM — are deduplicated, keyed on
 * `descriptor.serialName.removeSuffix("?")`. A LIST descriptor is never added to that seen-set:
 * every `List` and `Set` in the protocol shares one serial name regardless of element type, so
 * treating a LIST descriptor itself as "seen" would stop the walk from ever reaching whatever a
 * second list's element type leads to.
 *
 * Two different serial names whose last dotted segment agrees — a collision [typeNameOf] would
 * otherwise merge silently — throw [IllegalStateException] naming both.
 *
 * @param roots The descriptors to walk, in the order their declarations should appear.
 * @return Every reachable named declaration, each exactly once, in discovery order.
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun protocolDeclarations(
    roots: List<SerialDescriptor> = protocolRoots,
): List<TypeScriptDeclaration> {
    val declarations = mutableListOf<TypeScriptDeclaration>()
    val serialNamesSeen = mutableSetOf<String>()
    val shortNameOwners = mutableMapOf<String, String>()

    // Registers a named declaration's full serial name; false means it was already emitted, so
    // the caller must neither re-emit it nor re-walk its children. Never called for LIST
    // descriptors: see the seen-set rule in the function doc above.
    fun claimName(descriptor: SerialDescriptor): Boolean {
        val serialName = descriptor.serialName.removeSuffix("?")
        if (!serialNamesSeen.add(serialName)) return false

        val shortName = typeNameOf(descriptor)
        val owner = shortNameOwners[shortName]
        if (owner == null) {
            shortNameOwners[shortName] = serialName
        } else if (owner != serialName) {
            throw IllegalStateException(
                "Short name collision: \"$owner\" and \"$serialName\" both resolve to \"$shortName\"",
            )
        }
        return true
    }

    fun walk(descriptor: SerialDescriptor, discriminator: String?) {
        when (descriptor.kind) {
            PolymorphicKind.SEALED -> {
                if (!claimName(descriptor)) return
                declarations += TypeScriptDeclaration(typeNameOf(descriptor), unionDeclaration(descriptor))
                sealedVariants(descriptor).forEach { (serialName, variant) -> walk(variant, serialName) }
            }
            StructureKind.CLASS, StructureKind.OBJECT -> {
                if (!claimName(descriptor)) return
                declarations += TypeScriptDeclaration(
                    typeNameOf(descriptor),
                    interfaceDeclaration(descriptor, discriminator),
                )
                for (index in 0 until descriptor.elementsCount) {
                    walk(descriptor.getElementDescriptor(index), null)
                }
            }
            SerialKind.ENUM -> {
                if (!claimName(descriptor)) return
                declarations += TypeScriptDeclaration(typeNameOf(descriptor), enumDeclaration(descriptor))
            }
            StructureKind.LIST -> walk(descriptor.getElementDescriptor(0), null)
            is PrimitiveKind -> Unit
            else -> throw IllegalArgumentException(
                "Unsupported descriptor kind: ${descriptor.kind} (serialName: ${descriptor.serialName})",
            )
        }
    }

    roots.forEach { walk(it, null) }
    return declarations
}
