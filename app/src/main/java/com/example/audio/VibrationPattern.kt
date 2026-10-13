package com.example.audio

/**
 * Unique and complex vibration patterns for microphone recording.
 * Each pattern has a distinct tactile persona, descriptive explanation, and complex waveform.
 */
data class VibrationPattern(
    val id: String,
    val name: String,
    val persona: String,
    val explanation: String,
    val timings: LongArray,
    val amplitudes: IntArray,
    val repeatIndex: Int
)

object VibrationPatterns {
    val OCEAN_SURGE = VibrationPattern(
        id = "ocean_surge",
        name = "Ocean Surge",
        persona = "The Steady Ocean",
        explanation = "Begins with a crisp tidal kick, smoothly recedes into calm water, then rhythmically builds and ebbs in continuous harmonic swells while held.",
        timings = longArrayOf(
            0, 70, 70,                               // kick + brief gap
            55, 65, 45, 65, 35, 75, 25, 80,          // decrescendo
            35, 75, 45, 65, 55, 65, 60, 65           // crescendo
        ),
        amplitudes = intArrayOf(
            0, 185, 0,
            140, 0, 90, 0, 55, 0, 25, 0,
            55, 0, 90, 0, 140, 0, 175, 0
        ),
        repeatIndex = 3
    )

    val CYBERNETIC_PULSE = VibrationPattern(
        id = "cybernetic_pulse",
        name = "Cybernetic Pulse",
        persona = "The Quantum Reactor",
        explanation = "Rapid double-staccato activation blips followed by a high-frequency syncopated polyrhythm that feels like a pressurized fusion reactor humming under your thumb.",
        timings = longArrayOf(
            0, 35, 40, 35, 80,                       // double activation kick
            65, 30, 25, 35, 45, 25, 20, 90,          // pulse beat 1
            65, 25, 20, 30, 50, 30, 20, 100          // pulse beat 2
        ),
        amplitudes = intArrayOf(
            0, 230, 0, 230, 0,
            190, 0, 110, 0, 160, 0, 90, 0,
            210, 0, 100, 0, 170, 0, 80, 0
        ),
        repeatIndex = 5
    )

    val HEARTBEAT_RESONANCE = VibrationPattern(
        id = "heartbeat_resonance",
        name = "Heartbeat Resonance",
        persona = "The Living Cardiac",
        explanation = "Emulates an intimate anatomical cardiac cycle: a deep visceral 'lub' impact followed immediately by a softer 'dub', then an organic resting pause before the next stroke.",
        timings = longArrayOf(
            0, 80, 50,                               // lub
            55, 460                                  // dub + resting lull
        ),
        amplitudes = intArrayOf(
            0, 220, 0,
            130, 0
        ),
        repeatIndex = 1
    )

    val METRONOME_GROOVE = VibrationPattern(
        id = "metronome_groove",
        name = "Metronome Groove",
        persona = "The Jazz Percussionist",
        explanation = "Syncopated swing tempo featuring an accented downbeat kick followed by delicate ghost-notes and triplet taps that keep an addictive cadence while you speak.",
        timings = longArrayOf(
            0, 90, 130,                              // accented downbeat
            25, 60,                                  // ghost tap 1
            25, 130,                                 // ghost tap 2
            40, 180                                  // syncopated rimshot + measure gap
        ),
        amplitudes = intArrayOf(
            0, 225, 0,
            75, 0,
            85, 0,
            150, 0
        ),
        repeatIndex = 1
    )

    val ELECTRIC_AURORA = VibrationPattern(
        id = "electric_aurora",
        name = "Electric Aurora",
        persona = "The Shimmering Ion",
        explanation = "Ascending stair-step micro-frequencies that escalate from a subtle tingling hum into high-voltage buzzes, creating a sensation like holding an electric charge.",
        timings = longArrayOf(
            0, 20, 55,                               // step 1
            20, 45,                                  // step 2
            20, 35,                                  // step 3
            20, 25,                                  // step 4
            25, 15,                                  // step 5
            30, 15,                                  // step 6 (crest)
            60, 40,                                  // discharge burst
            30, 40,
            20, 120                                  // electric decay
        ),
        amplitudes = intArrayOf(
            0, 60, 0,
            90, 0,
            130, 0,
            175, 0,
            220, 0,
            255, 0,
            200, 0,
            120, 0,
            60, 0
        ),
        repeatIndex = 1
    )

    val DEEP_RUMBLE = VibrationPattern(
        id = "deep_rumble",
        name = "Deep Rumble",
        persona = "The Tectonic Bass",
        explanation = "Heavy, low-end seismic throb with prolonged sustained bass pulses that feel powerful, grounded, and unmistakably physical like a subwoofer.",
        timings = longArrayOf(
            0, 180, 90,                              // strike 1
            130, 80,                                 // secondary rumble
            240, 190,                                // heavy seismic wave
            70, 250                                  // echo tremor + rest
        ),
        amplitudes = intArrayOf(
            0, 230, 0,
            180, 0,
            250, 0,
            110, 0
        ),
        repeatIndex = 1
    )

    val MORSE_TELEGRAPH = VibrationPattern(
        id = "morse_telegraph",
        name = "Morse Telegraph",
        persona = "The Secret Cipher",
        explanation = "Crisp, rhythmic vintage Morse code sequence encoding tactile dots and dashes with antique telegraph relay precision.",
        timings = longArrayOf(
            0, 30, 45,                               // dot
            85, 45,                                  // dash
            30, 45,                                  // dot
            85, 45,                                  // dash
            30, 80,                                  // dot + character space
            85, 45,                                  // dash
            85, 200                                  // dash + message rest
        ),
        amplitudes = intArrayOf(
            0, 210, 0,
            210, 0,
            210, 0,
            210, 0,
            210, 0,
            210, 0,
            210, 0
        ),
        repeatIndex = 1
    )

    val CAT_PURR = VibrationPattern(
        id = "cat_purr",
        name = "Cat Purr",
        persona = "The Warm Companion",
        explanation = "Ultra-soft, comforting micro-tremors cycling in gentle breathing intervals. Designed to be relaxing, non-intrusive, and subtly reassuring during long thoughts.",
        timings = longArrayOf(
            0, 20, 18, 22, 18, 25, 18, 28, 18,       // breath inhale swell
            25, 18, 22, 18, 20, 60,                  // breath exhale
            18, 20, 22, 20, 25, 20, 22, 20, 18, 140  // quiet lull cycle
        ),
        amplitudes = intArrayOf(
            0, 45, 0, 60, 0, 80, 0, 95, 0,
            80, 0, 60, 0, 45, 0,
            40, 0, 55, 0, 70, 0, 55, 0, 40, 0
        ),
        repeatIndex = 1
    )

    val CASCADE_WATERFALL = VibrationPattern(
        id = "cascade_waterfall",
        name = "Cascade Waterfall",
        persona = "The Accelerating Droplet",
        explanation = "Starts with isolated heavy droplet taps that steadily accelerate in speed into a continuous tumbling torrent, resetting in an endlessly satisfying loop.",
        timings = longArrayOf(
            0, 45, 250,                              // droplet 1
            40, 175,                                 // droplet 2
            35, 115,                                 // droplet 3
            30, 75,                                  // droplet 4
            25, 45,                                  // droplet 5
            25, 25,                                  // droplet 6
            120, 280                                 // waterfall surge + cycle reset
        ),
        amplitudes = intArrayOf(
            0, 220, 0,
            200, 0,
            180, 0,
            160, 0,
            150, 0,
            150, 0,
            190, 0
        ),
        repeatIndex = 1
    )

    val COSMIC_ORBIT = VibrationPattern(
        id = "cosmic_orbit",
        name = "Cosmic Orbit",
        persona = "The Gravitational Slingshot",
        explanation = "Simulates a celestial body swinging around a gravity well: a gentle ambient drift that builds into an intense close-flyby crescendo before slinging out into space.",
        timings = longArrayOf(
            0, 35, 80,                               // drift 1
            45, 70,                                  // drift 2
            60, 50,                                  // approach
            85, 35,                                  // near flyby
            120, 25,                                 // periapsis peak
            35, 80,                                  // whip snap
            40, 110,                                 // receding tail
            30, 250                                  // deep space pause
        ),
        amplitudes = intArrayOf(
            0, 40, 0,
            70, 0,
            115, 0,
            170, 0,
            250, 0,
            230, 0,
            90, 0,
            45, 0
        ),
        repeatIndex = 1
    )

    val ALL: List<VibrationPattern> = listOf(
        OCEAN_SURGE,
        CYBERNETIC_PULSE,
        HEARTBEAT_RESONANCE,
        METRONOME_GROOVE,
        ELECTRIC_AURORA,
        DEEP_RUMBLE,
        MORSE_TELEGRAPH,
        CAT_PURR,
        CASCADE_WATERFALL,
        COSMIC_ORBIT
    )

    fun findById(id: String): VibrationPattern {
        return ALL.find { it.id == id } ?: OCEAN_SURGE
    }
}
