package com.aistudio.pianobar.kxuvzq.pandora

import android.util.Log
import com.aistudio.pianobar.kxuvzq.model.Station
import com.aistudio.pianobar.kxuvzq.model.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

sealed class PandoraAuthResult {
    data class Success(
        val userAuthToken: String,
        val userId: String,
        val stations: List<Station>
    ) : PandoraAuthResult()

    data class Error(val message: String, val code: Int? = null) : PandoraAuthResult()
}

/**
 * 1:1 port of Pianobar (2024.12.21) libpiano request engine:
 * - auth.partnerLogin (step 0): unencrypted, returns partnerAuthToken, partnerId, syncTime
 * - timeOffset calculation: localRealTimestamp - serverTimestamp
 * - auth.userLogin (step 1): encrypted with partnerAuthToken inside body, auth_token and partner_id in query string
 * - user.getStationList: encrypted with userAuthToken and syncTime, method=user.getStationList&auth_token=...&partner_id=...&user_id=...
 * - station.getPlaylist: encrypted with userAuthToken, stationToken, syncTime, method=station.getPlaylist&auth_token=...&partner_id=...&user_id=...
 */
class PandoraApiClient {

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(6, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .writeTimeout(6, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()

    private val baseEndpoint = "https://tuner.pandora.com/services/json/"
    private val textPlainMedia = "text/plain; charset=utf-8".toMediaType()

    private var partnerId: String? = null
    private var partnerAuthToken: String? = null
    private var userAuthToken: String? = null
    private var userId: String? = null
    private var timeOffsetSec: Long = 0L // ph->timeOffset = realTimestamp - serverTimestamp

    // Stored credentials for automatic session re-establishment after network reconnects (e.g. VPN/Tailscale)
    private var lastUsername: String? = null
    private var lastPassword: String? = null

    fun resetNetworkConnections() {
        try {
            httpClient.connectionPool.evictAll()
            httpClient.dispatcher.cancelAll()
            Log.i("PandoraApiClient", "Evicted all cached HTTP connections and canceled pending calls.")
        } catch (e: Exception) {
            Log.w("PandoraApiClient", "Error evicting connections: ${e.message}")
        }
    }

    fun clearSession() {
        partnerId = null
        partnerAuthToken = null
        userAuthToken = null
        userId = null
        timeOffsetSec = 0L
        lastUsername = null
        lastPassword = null
        resetNetworkConnections()
    }

    suspend fun reauthenticateIfNeeded(): Boolean = withContext(Dispatchers.IO) {
        val user = lastUsername
        val pass = lastPassword
        if (!user.isNullOrBlank() && !pass.isNullOrBlank()) {
            resetNetworkConnections()
            val result = loginAndFetchStations(user, pass)
            result is PandoraAuthResult.Success
        } else {
            false
        }
    }

    /**
     * Calculates time_t timestamp = time(NULL) - ph->timeOffset as in request.c
     */
    private fun getCurrentSyncTime(): Long {
        val nowSec = System.currentTimeMillis() / 1000L
        return nowSec - timeOffsetSec
    }

    suspend fun loginAndFetchStations(user: String, pass: String): PandoraAuthResult = withContext(Dispatchers.IO) {
        lastUsername = user.trim()
        lastPassword = pass.trim()
        try {
            // Step 0: auth.partnerLogin
            val partnerError = executePartnerLogin()
            if (partnerError != null) {
                return@withContext PandoraAuthResult.Error(partnerError)
            }

            val pToken = partnerAuthToken ?: return@withContext PandoraAuthResult.Error("Missing partner token")
            val pId = partnerId ?: return@withContext PandoraAuthResult.Error("Missing partner ID")

            // Step 1: auth.userLogin (libpiano request.c: case PIANO_REQUEST_LOGIN, step 1)
            val userPayload = JSONObject().apply {
                put("loginType", "user")
                put("username", user.trim())
                put("password", pass.trim())
                put("partnerAuthToken", pToken)
                put("syncTime", getCurrentSyncTime())
            }.toString()

            val encryptedUserPayload = PandoraBlowfishCrypto.encrypt(userPayload)
            val userUrl = "$baseEndpoint?method=auth.userLogin&auth_token=${URLEncoder.encode(pToken, "UTF-8")}&partner_id=$pId"

            val req = Request.Builder()
                .url(userUrl)
                .post(encryptedUserPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val (isSuccessful, code, respBody) = httpClient.newCall(req).execute().use { response ->
                Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
            }

            if (!isSuccessful) {
                return@withContext PandoraAuthResult.Error("Pandora server HTTP $code")
            }

            val json = JSONObject(respBody)
            val stat = json.optString("stat", "fail")
            if (stat != "ok") {
                val errorCode = json.optInt("code", -1)
                val serverMsg = json.optString("message", "Authentication rejected")
                // libpiano response.c: step 1 error 1002 is remapped to PIANO_RET_INVALID_LOGIN ("Invalid username and/or password.")
                val message = if (errorCode == 1002) {
                    "Invalid username and/or password (Code 1002)"
                } else {
                    "$serverMsg (Code $errorCode)"
                }
                return@withContext PandoraAuthResult.Error(message, errorCode)
            }

            val resultObj = json.getJSONObject("result")
            val uAuthToken = resultObj.getString("userAuthToken")
            val uId = resultObj.getString("userId")

            this@PandoraApiClient.userAuthToken = uAuthToken
            this@PandoraApiClient.userId = uId
            Log.i("PandoraApiClient", "User login succeeded. userId=$uId")

            // Step 2: user.getStationList
            val stations = fetchUserStations(uAuthToken, pId, uId)
            PandoraAuthResult.Success(
                userAuthToken = uAuthToken,
                userId = uId,
                stations = stations
            )
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error in login flow", e)
            PandoraAuthResult.Error(e.message ?: "Network error connecting to Pandora tuner")
        }
    }

    private fun executePartnerLogin(): String? {
        val partnerJson = JSONObject().apply {
            put("username", PandoraBlowfishCrypto.PARTNER_USERNAME)
            put("password", PandoraBlowfishCrypto.PARTNER_PASSWORD)
            put("deviceModel", PandoraBlowfishCrypto.DEVICE_MODEL)
            put("version", PandoraBlowfishCrypto.PROTOCOL_VERSION)
            put("includeUrls", true)
        }.toString()

        val partnerUrl = "$baseEndpoint?method=auth.partnerLogin"
        val req = Request.Builder()
            .url(partnerUrl)
            .post(partnerJson.toRequestBody(textPlainMedia))
            .header("User-Agent", "pianobar-2024.12.21")
            .build()

        val localRealTimestamp = System.currentTimeMillis() / 1000L
        val (isSuccessful, code, respBody) = httpClient.newCall(req).execute().use { response ->
            Triple(response.isSuccessful, response.code, response.body?.string() ?: "")
        }
        if (!isSuccessful) {
            return "Partner handshake rejected with HTTP $code"
        }

        val json = JSONObject(respBody)
        if (json.optString("stat") != "ok") {
            return json.optString("message", "Partner handshake failed")
        }

        val resultObj = json.getJSONObject("result")
        partnerId = resultObj.getString("partnerId")
        partnerAuthToken = resultObj.getString("partnerAuthToken")
        val syncTimeHex = resultObj.getString("syncTime")

        // In libpiano response.c: ph->timeOffset = realTimestamp - serverTimestamp
        val serverTimestamp = PandoraBlowfishCrypto.decryptSyncTime(syncTimeHex)
        timeOffsetSec = localRealTimestamp - serverTimestamp
        Log.i("PandoraApiClient", "Partner login ok. partnerId=$partnerId, serverTimestamp=$serverTimestamp, timeOffset=$timeOffsetSec")
        return null
    }

    suspend fun fetchUserStations(
        uAuthToken: String = userAuthToken ?: "",
        pId: String = partnerId ?: "",
        uId: String = userId ?: ""
    ): List<Station> = withContext(Dispatchers.IO) {
        if (uAuthToken.isEmpty() || pId.isEmpty()) return@withContext emptyList()

        try {
            // libpiano request.c: case PIANO_REQUEST_GET_STATIONS
            val stationListPayload = JSONObject().apply {
                put("userAuthToken", uAuthToken)
                put("syncTime", getCurrentSyncTime())
                put("returnAllStations", true)
            }.toString()

            val encryptedPayload = PandoraBlowfishCrypto.encrypt(stationListPayload)
            val stationUrl = "$baseEndpoint?method=user.getStationList&auth_token=${URLEncoder.encode(uAuthToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

            val req = Request.Builder()
                .url(stationUrl)
                .post(encryptedPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val respBody = httpClient.newCall(req).execute().use { response ->
                response.body?.string() ?: ""
            }
            val json = JSONObject(respBody)

            if (json.optString("stat") != "ok") {
                Log.w("PandoraApiClient", "Failed to retrieve stations: $respBody")
                return@withContext emptyList()
            }

            val resultObj = json.getJSONObject("result")
            val stationsArray = resultObj.optJSONArray("stations") ?: return@withContext emptyList()
            val list = mutableListOf<Station>()

            for (i in 0 until stationsArray.length()) {
                val stObj = stationsArray.getJSONObject(i)
                val stId = stObj.optString("stationId", "station_$i")
                val stToken = stObj.optString("stationToken", stId)
                val stName = stObj.optString("stationName", "Station $i")
                val isQuickMix = stObj.optBoolean("isQuickMix", false)

                list.add(
                    Station(
                        id = stToken, // Use stationToken for station.getPlaylist
                        name = stName,
                        description = "Pandora Station: $stName",
                        genre = if (isQuickMix) "QuickMix" else "Pandora Radio",
                        isQuickMix = isQuickMix,
                        isCustom = !isQuickMix,
                        defaultTracks = emptyList()
                    )
                )
            }
            list
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error fetching station list", e)
            emptyList()
        }
    }

    suspend fun fetchStationPlaylist(stationTokenOrId: String): List<Track> = withContext(Dispatchers.IO) {
        if (userAuthToken == null || partnerId == null) {
            if (!reauthenticateIfNeeded()) {
                return@withContext emptyList()
            }
        }
        val uToken = userAuthToken ?: return@withContext emptyList()
        val pId = partnerId ?: return@withContext emptyList()
        val uId = userId ?: ""

        try {
            // libpiano request.c: case PIANO_REQUEST_GET_PLAYLIST
            val playlistPayload = JSONObject().apply {
                put("userAuthToken", uToken)
                put("stationToken", stationTokenOrId)
                put("includeTrackLength", true)
                put("additionalAudioUrl", "HTTP_128_MP3,HTTP_64_AACPLUS_ADTS,HTTP_32_AACPLUS_ADTS")
                put("syncTime", getCurrentSyncTime())
            }.toString()

            val encryptedPayload = PandoraBlowfishCrypto.encrypt(playlistPayload)
            val playlistUrl = "$baseEndpoint?method=station.getPlaylist&auth_token=${URLEncoder.encode(uToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

            val req = Request.Builder()
                .url(playlistUrl)
                .post(encryptedPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val respBody = httpClient.newCall(req).execute().use { response ->
                response.body?.string() ?: ""
            }
            val json = JSONObject(respBody)

            if (json.optString("stat") != "ok") {
                val errCode = json.optInt("code", 0)
                Log.w("PandoraApiClient", "Failed to retrieve playlist: $respBody")
                // Session expired / auth invalid -> re-authenticate and retry once
                if (errCode == 1003 || errCode == 1001 || errCode == 1000) {
                    Log.i("PandoraApiClient", "Session token expired (code $errCode). Re-authenticating...")
                    if (reauthenticateIfNeeded()) {
                        return@withContext fetchStationPlaylistInternal(stationTokenOrId)
                    }
                }
                return@withContext emptyList()
            }

            parsePlaylistTracks(json, stationTokenOrId)
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error fetching playlist: ${e.message}. Attempting network reset and re-auth...", e)
            resetNetworkConnections()
            // If network route changed (e.g. Tailscale or VPN toggled), re-authenticate and retry once
            try {
                if (reauthenticateIfNeeded()) {
                    Log.i("PandoraApiClient", "Re-authenticated after network error. Retrying playlist fetch...")
                    return@withContext fetchStationPlaylistInternal(stationTokenOrId)
                }
            } catch (retryEx: Exception) {
                Log.e("PandoraApiClient", "Retry failed: ${retryEx.message}")
            }
            emptyList()
        }
    }

    private fun fetchStationPlaylistInternal(stationTokenOrId: String): List<Track> {
        val uToken = userAuthToken ?: return emptyList()
        val pId = partnerId ?: return emptyList()
        val uId = userId ?: ""

        val playlistPayload = JSONObject().apply {
            put("userAuthToken", uToken)
            put("stationToken", stationTokenOrId)
            put("includeTrackLength", true)
            put("additionalAudioUrl", "HTTP_128_MP3,HTTP_64_AACPLUS_ADTS,HTTP_32_AACPLUS_ADTS")
            put("syncTime", getCurrentSyncTime())
        }.toString()

        val encryptedPayload = PandoraBlowfishCrypto.encrypt(playlistPayload)
        val playlistUrl = "$baseEndpoint?method=station.getPlaylist&auth_token=${URLEncoder.encode(uToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

        val req = Request.Builder()
            .url(playlistUrl)
            .post(encryptedPayload.toRequestBody(textPlainMedia))
            .header("User-Agent", "pianobar-2024.12.21")
            .build()

        val respBody = httpClient.newCall(req).execute().use { response ->
            response.body?.string() ?: ""
        }
        val json = JSONObject(respBody)
        if (json.optString("stat") != "ok") return emptyList()
        return parsePlaylistTracks(json, stationTokenOrId)
    }

    private fun parsePlaylistTracks(json: JSONObject, stationTokenOrId: String): List<Track> {
        val resultObj = json.optJSONObject("result") ?: return emptyList()
        val itemsArray = resultObj.optJSONArray("items") ?: return emptyList()
        val tracks = mutableListOf<Track>()
        val batchFetchedAtMs = System.currentTimeMillis()

        for (i in 0 until itemsArray.length()) {
            val item = itemsArray.getJSONObject(i)
            val songTitle = item.optString("songName", "")
            if (songTitle.isBlank()) continue

            val artistName = item.optString("artistName", "Unknown Artist")
            val albumName = item.optString("albumName", "Pandora Radio")
            val artUrl = item.optString("albumArtUrl", "")
            val trackToken = item.optString("trackToken", "track_$i")
            val songDuration = item.optLong("songDuration", 180L) * 1000L

            var streamUrl = ""
            var audioBitrate = "128 kbps"
            if (item.has("audioUrlMap")) {
                val urlMap = item.getJSONObject("audioUrlMap")
                val qualityKey = when {
                    urlMap.has("highQuality") -> "highQuality"
                    urlMap.has("mediumQuality") -> "mediumQuality"
                    urlMap.has("lowQuality") -> "lowQuality"
                    else -> null
                }
                if (qualityKey != null) {
                    val qObj = urlMap.getJSONObject(qualityKey)
                    streamUrl = qObj.optString("audioUrl", "")
                    val encoding = qObj.optString("encoding", "aacplus")
                    val bitrate = qObj.optString("bitrate", if (qualityKey == "highQuality") "128" else "64")
                    audioBitrate = "$encoding $bitrate kbps"
                }
            }
            if (streamUrl.isBlank()) {
                streamUrl = item.optString("audioUrl", "")
            }

            if (streamUrl.isNotBlank()) {
                tracks.add(
                    Track(
                        id = trackToken,
                        title = songTitle,
                        artist = artistName,
                        album = albumName,
                        artworkUrl = artUrl.ifBlank { "https://images.unsplash.com/photo-1511671782779-c97d3d27a1d4?w=600&auto=format&fit=crop&q=80" },
                        durationMs = songDuration,
                        streamUrl = streamUrl,
                        stationId = stationTokenOrId,
                        musicGenomeExplanation = "Live stream served by Pandora Music Genome for station '$stationTokenOrId'.",
                        audioBitrate = audioBitrate,
                        year = item.optString("year", ""),
                        fetchedAtMs = batchFetchedAtMs
                    )
                )
            }
        }
        return tracks
    }

    /**
     * Mirrors libpiano PIANO_REQUEST_EXPLAIN (request.c line 315 & response.c line 516):
     * method = "track.explainTrack"
     * Payload: { "trackToken": trackToken, "userAuthToken": uToken, "syncTime": syncTime }
     * Returns: "We're playing this track because it features trait1, trait2 and trait3."
     */
    suspend fun explainTrack(trackToken: String): String? = withContext(Dispatchers.IO) {
        val uToken = userAuthToken ?: return@withContext null
        val pId = partnerId ?: return@withContext null
        val uId = userId ?: ""

        try {
            val payload = JSONObject().apply {
                put("userAuthToken", uToken)
                put("trackToken", trackToken)
                put("syncTime", getCurrentSyncTime())
            }.toString()

            val encryptedPayload = PandoraBlowfishCrypto.encrypt(payload)
            val explainUrl = "$baseEndpoint?method=track.explainTrack&auth_token=${URLEncoder.encode(uToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

            val req = Request.Builder()
                .url(explainUrl)
                .post(encryptedPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val response = httpClient.newCall(req).execute()
            val respBody = response.body?.string() ?: ""
            val json = JSONObject(respBody)

            if (json.optString("stat") != "ok") {
                Log.w("PandoraApiClient", "track.explainTrack failed: $respBody")
                return@withContext null
            }

            val resultObj = json.getJSONObject("result")
            val explanationsArray = resultObj.optJSONArray("explanations") ?: return@withContext null
            if (explanationsArray.length() == 0) return@withContext null

            val traits = mutableListOf<String>()
            for (i in 0 until explanationsArray.length()) {
                val expItem = explanationsArray.getJSONObject(i)
                val trait = expItem.optString("focusTraitName", "")
                if (trait.isNotBlank()) {
                    traits.add(trait)
                }
            }

            if (traits.isEmpty()) return@withContext null

            // Format exactly like libpiano response.c:
            // "We're playing this track because it features trait1, trait2 and trait3."
            val sb = StringBuilder("We're playing this track because it features ")
            for (i in traits.indices) {
                sb.append(traits[i])
                when {
                    i < traits.size - 2 -> sb.append(", ")
                    i == traits.size - 2 -> sb.append(" and ")
                    else -> sb.append(".")
                }
            }
            sb.toString()
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error explaining track", e)
            null
        }
    }

    suspend fun addFeedback(stationToken: String, trackToken: String, isPositive: Boolean): Boolean = withContext(Dispatchers.IO) {
        val uToken = userAuthToken ?: return@withContext false
        val pId = partnerId ?: return@withContext false
        val uId = userId ?: ""

        try {
            val payload = JSONObject().apply {
                put("userAuthToken", uToken)
                put("stationToken", stationToken)
                put("trackToken", trackToken)
                put("isPositive", isPositive)
                put("syncTime", getCurrentSyncTime())
            }.toString()

            val encryptedPayload = PandoraBlowfishCrypto.encrypt(payload)
            val feedbackUrl = "$baseEndpoint?method=station.addFeedback&auth_token=${URLEncoder.encode(uToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

            val req = Request.Builder()
                .url(feedbackUrl)
                .post(encryptedPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val response = httpClient.newCall(req).execute()
            val respBody = response.body?.string() ?: ""
            val json = JSONObject(respBody)
            json.optString("stat") == "ok"
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error adding feedback", e)
            false
        }
    }

    suspend fun addSongBookmark(trackToken: String): Boolean = withContext(Dispatchers.IO) {
        val uToken = userAuthToken ?: return@withContext false
        val pId = partnerId ?: return@withContext false
        val uId = userId ?: ""

        try {
            val payload = JSONObject().apply {
                put("userAuthToken", uToken)
                put("trackToken", trackToken)
                put("syncTime", getCurrentSyncTime())
            }.toString()

            val encryptedPayload = PandoraBlowfishCrypto.encrypt(payload)
            val bookmarkUrl = "$baseEndpoint?method=bookmark.addSongBookmark&auth_token=${URLEncoder.encode(uToken, "UTF-8")}&partner_id=$pId&user_id=${URLEncoder.encode(uId, "UTF-8")}"

            val req = Request.Builder()
                .url(bookmarkUrl)
                .post(encryptedPayload.toRequestBody(textPlainMedia))
                .header("User-Agent", "pianobar-2024.12.21")
                .build()

            val response = httpClient.newCall(req).execute()
            val respBody = response.body?.string() ?: ""
            val json = JSONObject(respBody)
            json.optString("stat") == "ok"
        } catch (e: Exception) {
            Log.e("PandoraApiClient", "Error adding bookmark", e)
            false
        }
    }
}
