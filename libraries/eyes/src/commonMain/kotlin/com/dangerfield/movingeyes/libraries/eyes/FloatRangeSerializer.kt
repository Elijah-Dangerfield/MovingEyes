package com.dangerfield.movingeyes.libraries.eyes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * kotlinx.serialization can't handle `ClosedFloatingPointRange` on its own —
 * the concrete type isn't `@Serializable` and the interface isn't sealed, so a
 * class containing one compiles and then throws on first encode.
 */
object FloatRangeSerializer : KSerializer<ClosedFloatingPointRange<Float>> {

    @Serializable
    private data class Surrogate(val from: Float, val to: Float)

    override val descriptor: SerialDescriptor = Surrogate.serializer().descriptor

    override fun serialize(encoder: Encoder, value: ClosedFloatingPointRange<Float>) {
        encoder.encodeSerializableValue(
            Surrogate.serializer(),
            Surrogate(from = value.start, to = value.endInclusive),
        )
    }

    override fun deserialize(decoder: Decoder): ClosedFloatingPointRange<Float> {
        val surrogate = decoder.decodeSerializableValue(Surrogate.serializer())
        return surrogate.from..surrogate.to
    }
}
