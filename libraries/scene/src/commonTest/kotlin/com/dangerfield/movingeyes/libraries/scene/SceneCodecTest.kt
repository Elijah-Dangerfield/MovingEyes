package com.dangerfield.movingeyes.libraries.scene

import com.dangerfield.movingeyes.libraries.eyes.BehaviorConfig
import com.dangerfield.movingeyes.libraries.eyes.EyeStyleId
import com.dangerfield.movingeyes.libraries.eyes.Mood
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The codec is the only thing standing between a release and destroying work
 * the user can't recreate, so it's tested against the payloads it will actually
 * meet: its own output, a payload from a future build, and a corrupted one.
 */
class SceneCodecTest {

    private val scene = Scene(
        id = "abc",
        name = "Front window",
        eyes = listOf(
            SceneEye(
                styleId = EyeStyleId.Demon,
                x = 0.4f,
                y = 0.62f,
                sizeFraction = 0.18f,
                rotationDegrees = 15f,
                scleraColor = 0xFF3A1A0E,
                irisColor = 0xFFF07A1E,
                pupilColor = 0xFF000000,
                glowFraction = 0.18f,
                mood = Mood.Dormant,
            ),
        ),
        canvasRotation = CanvasRotation.Half,
        brightness = 0.4f,
    )

    @Test
    fun `a scene survives a round trip unchanged`() {
        val decoded = SceneCodec.decode(SceneCodec.encode(scene))

        assertIs<SceneDecodeResult.Success>(decoded)
        assertEquals(scene, decoded.scene)
    }

    @Test
    fun `a custom behaviour survives a round trip`() {
        val custom = scene.copy(
            eyes = listOf(
                scene.eyes.first().copy(
                    mood = Mood.Custom,
                    customBehavior = BehaviorConfig(gazeRange = 0.77f, jitter = 0.05f),
                ),
            ),
        )

        val decoded = SceneCodec.decode(SceneCodec.encode(custom))

        assertIs<SceneDecodeResult.Success>(decoded)
        assertEquals(0.77f, decoded.scene.eyes.first().customBehavior?.gazeRange)
    }

    @Test
    fun `the envelope carries the current version`() {
        assertTrue(SceneCodec.encode(scene).contains("\"v\":${SceneCodec.CurrentVersion}"))
    }

    /**
     * The forward-compatibility guarantee that lets a later release add an
     * optional field without writing a migration for it.
     */
    @Test
    fun `an unknown field written by a later build is ignored, not fatal`() {
        val withExtra = SceneCodec.encode(scene)
            .replace("\"name\":", "\"somethingNewInV2\":true,\"name\":")

        val decoded = SceneCodec.decode(withExtra)

        assertIs<SceneDecodeResult.Success>(decoded)
        assertEquals(scene.name, decoded.scene.name)
    }

    /**
     * A newer payload is refused rather than guessed at. Showing a partially
     * understood composition would be worse than showing none, because the user
     * would then re-save it and overwrite the good copy.
     */
    @Test
    fun `a payload from a future version is refused rather than guessed at`() {
        val future = SceneCodec.encode(scene)
            .replace("\"v\":${SceneCodec.CurrentVersion}", "\"v\":${SceneCodec.CurrentVersion + 1}")

        val decoded = SceneCodec.decode(future)

        assertIs<SceneDecodeResult.FromTheFuture>(decoded)
        assertEquals(SceneCodec.CurrentVersion + 1, decoded.version)
        assertEquals(SceneCodec.CurrentVersion, decoded.supported)
    }

    @Test
    fun `garbage is unreadable rather than an exception`() {
        assertIs<SceneDecodeResult.Unreadable>(SceneCodec.decode("not json at all"))
        assertIs<SceneDecodeResult.Unreadable>(SceneCodec.decode("[1,2,3]"))
        assertIs<SceneDecodeResult.Unreadable>(SceneCodec.decode("{\"d\":{}}"))
        assertIs<SceneDecodeResult.Unreadable>(SceneCodec.decode("{\"v\":1}"))
    }

    @Test
    fun `a truncated payload is unreadable rather than a half-built scene`() {
        val encoded = SceneCodec.encode(scene)

        val decoded = SceneCodec.decode(encoded.substring(0, encoded.length / 2))

        assertIs<SceneDecodeResult.Unreadable>(decoded)
    }

    @Test
    fun `a non-custom mood does not carry a behaviour that could disagree with it`() {
        val decoded = SceneCodec.decode(SceneCodec.encode(scene))

        assertIs<SceneDecodeResult.Success>(decoded)
        assertNull(decoded.scene.eyes.first().customBehavior)
    }
}
