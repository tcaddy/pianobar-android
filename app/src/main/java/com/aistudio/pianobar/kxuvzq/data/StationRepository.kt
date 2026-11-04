package com.aistudio.pianobar.kxuvzq.data

import com.aistudio.pianobar.kxuvzq.model.Station
import com.aistudio.pianobar.kxuvzq.model.Track

object StationRepository {

    // No hardcoded audio files or test accounts. Stations and content are fetched directly from Pandora.
    val defaultStations: List<Station> = emptyList()

    fun createCustomStation(name: String, seed: String, genre: String): Station {
        val id = "station_custom_" + System.currentTimeMillis()
        return Station(
            id = id,
            name = name,
            description = "Station seeded by $seed",
            genre = genre.ifBlank { "Pandora Radio" },
            isQuickMix = true,
            isCustom = true,
            defaultTracks = emptyList()
        )
    }
}
