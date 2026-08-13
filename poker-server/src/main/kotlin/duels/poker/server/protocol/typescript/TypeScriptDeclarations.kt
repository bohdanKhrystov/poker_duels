package duels.poker.server.protocol.typescript

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * Emits a TypeScript interface declaration for a class or object descriptor.
 *
 * For StructureKind.CLASS, returns an `export interface` declaration with all elements as
 * properties. For StructureKind.OBJECT as a sealed variant, returns just the discriminator.
 *
 * @param descriptor The serial descriptor to emit
 * @param discriminator Optional discriminator value; if provided, emits `type: "$discriminator"` as the first property
 * @return A TypeScript interface declaration with no trailing newline
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun interfaceDeclaration(descriptor: SerialDescriptor, discriminator: String? = null): String {
    val interfaceName = typeNameOf(descriptor)
    val lines = mutableListOf<String>()
    lines.add("export interface $interfaceName {")

    // Add discriminator as first property if provided
    if (discriminator != null) {
        lines.add("  type: \"$discriminator\";")
    }

    // Add properties from descriptor elements
    for (i in 0 until descriptor.elementsCount) {
        val elementName = descriptor.getElementName(i)
        val elementDescriptor = descriptor.getElementDescriptor(i)
        val elementType = typeReference(elementDescriptor)
        lines.add("  $elementName: $elementType;")
    }

    lines.add("}")

    return lines.joinToString("\n")
}
