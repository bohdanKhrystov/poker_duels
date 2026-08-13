package duels.poker.server.protocol.typescript

import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * The serial names and descriptors of a sealed hierarchy's variants.
 *
 * Element 1 of a sealed descriptor is the `value` slot: its element names are the subtypes'
 * serial names and its element descriptors are the subtypes themselves. This function extracts
 * them as pairs in descriptor order, never navigating into the CONTEXTUAL value slot itself.
 *
 * @param descriptor A sealed descriptor
 * @return A list of (serialName, subtype descriptor) pairs in descriptor order
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun sealedVariants(descriptor: SerialDescriptor): List<Pair<String, SerialDescriptor>> {
    val value = descriptor.getElementDescriptor(1)
    return (0 until value.elementsCount).map {
        value.getElementName(it) to value.getElementDescriptor(it)
    }
}

/**
 * Emits a TypeScript union of string literals for an enum.
 *
 * @param descriptor An enum descriptor
 * @return One line: `export type Name = "ENTRY1" | "ENTRY2" | ...;`
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun enumDeclaration(descriptor: SerialDescriptor): String {
    val typeName = typeNameOf(descriptor)
    val entries = (0 until descriptor.elementsCount).map { descriptor.getElementName(it) }
    val union = entries.joinToString(" | ") { "\"$it\"" }
    return "export type $typeName = $union;"
}

/**
 * Emits a TypeScript union of variant type names for a sealed hierarchy.
 *
 * The variant names come from [sealedVariants] and are the same discriminator literals that
 * `interfaceDeclaration` writes, so the two cannot disagree.
 *
 * @param descriptor A sealed descriptor
 * @return One line: `export type Name = Variant1 | Variant2 | ...;`
 */
@OptIn(ExperimentalSerializationApi::class)
internal fun unionDeclaration(descriptor: SerialDescriptor): String {
    val typeName = typeNameOf(descriptor)
    val variantNames = sealedVariants(descriptor).map { it.first }
    val union = variantNames.joinToString(" | ")
    return "export type $typeName = $union;"
}

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
