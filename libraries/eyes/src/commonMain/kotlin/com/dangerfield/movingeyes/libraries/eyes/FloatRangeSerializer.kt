package com.dangerfield.movingeyes.libraries.eyes

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder

/**
 * Encodes a `ClosedFloatingPointRange<Float>` as `{"from":…,"to":…}`.
 *
 * kotlinx.serialization can't serialize the range interface on its own: the
 * concrete `ClosedFloatRange` isn't `@Serializable` and the interface isn't
 * sealed, so `@Serializable` on a class *containing* one compiles happily and
 * then throws at runtime the first time anybody encodes it.
 *
 * That matters here more than it looks, because [BehaviorConfig]'s ranges are
 * the fields that keep eyes from blinking in lockstep — see its own docs. They
 * are the reason a custom mood is worth saving at all, so "saving a custom mood
 * throws" is not a corner case.
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
