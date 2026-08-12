package duels.poker.engine.card

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

public object CardSerializer : KSerializer<Card> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("duels.poker.engine.card.Card", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: Card) {
        encoder.encodeString(value.toString())
    }

    override fun deserialize(decoder: Decoder): Card = Card.parse(decoder.decodeString())
}
