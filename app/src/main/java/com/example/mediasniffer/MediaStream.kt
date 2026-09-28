package com.example.mediasniffer

enum class MediaType {
    VIDEO,
    AUDIO,
    STREAM_PLAYLIST // HLS / DASH (.m3u8, .mpd)
}

data class MediaStream(
    val url: String,
    val type: MediaType,
    val mimeType: String,
    val extension: String
)
