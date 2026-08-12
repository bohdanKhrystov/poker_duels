package duels.poker.server.protocol

import kotlinx.serialization.descriptors.SerialDescriptor

/**
 * The `@SerialName` of every subtype of a sealed hierarchy, from its parent descriptor.
 *
 * Element 1 of a sealed descriptor is the `value` slot: its element names are the subtypes'
 * serial names (the discriminators that appear on the wire) and its element descriptors are the
 * subtypes themselves. Walking this is structural — it survives a Kotlin class rename that a
 * hand-maintained list of "today's subtypes" would not.
 */
internal fun subtypeNames(parent: SerialDescriptor): List<String> {
    val value = parent.getElementDescriptor(1)
    return (0 until value.elementsCount).map { value.getElementName(it) }
}

/** Each subtype's own descriptor, in the same order as [subtypeNames]. */
internal fun subtypeDescriptors(parent: SerialDescriptor): List<SerialDescriptor> {
    val value = parent.getElementDescriptor(1)
    return (0 until value.elementsCount).map { value.getElementDescriptor(it) }
}
