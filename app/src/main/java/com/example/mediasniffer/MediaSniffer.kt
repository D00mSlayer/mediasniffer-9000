package com.example.mediasniffer

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.webkit.WebResourceRequest
import android.webkit.WebResourceResponse
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class MediaSniffer(private val context: Context) {

    private val _streams = MutableStateFlow<List<MediaStream>>(emptyList())
    val streams: StateFlow<List<MediaStream>> = _streams.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private var webView: WebView? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    @SuppressLint("SetJavaScriptEnabled")
    fun startSniffing(url: String) {
        mainHandler.post {
            _streams.value = emptyList()
            _isLoading.value = true

            webView?.destroy()
            webView = WebView(context).apply {
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString =
                    "Mozilla/5.0 (Linux; Android 16; Mobile) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/130.0.0.0 Mobile Safari/537.36"

                webViewClient = object : WebViewClient() {
                    override fun shouldInterceptRequest(
                        view: WebView?,
                        request: WebResourceRequest?
                    ): WebResourceResponse? {
                        request?.url?.toString()?.let { reqUrl ->
                            inspectUrl(reqUrl)
                        }
                        return super.shouldInterceptRequest(view, request)
                    }

                    override fun onPageFinished(view: WebView?, finishedUrl: String?) {
                        super.onPageFinished(view, finishedUrl)
                        _isLoading.value = false
                        extractDomMedia(view)
                    }
                }

                loadUrl(url)
            }
        }
    }

    private fun extractDomMedia(view: WebView?) {
        val jsScript = """
            (function() {
                var mediaUrls = [];
                var elements = document.querySelectorAll('video, audio, source');
                elements.forEach(function(el) {
                    if (el.src && el.src.startsWith('http')) {
                        mediaUrls.push(el.src);
                    }
                });
                return JSON.stringify(mediaUrls);
            })();
        """.trimIndent()

        view?.evaluateJavascript(jsScript) { result ->
            if (result != null && result != "null" && result != "[]") {
                val cleaned = result.removeSurrounding("\"", "\"")
                    .replace("\\\"", "\"")
                    .removeSurrounding("[", "]")

                val urls = cleaned.split(",").map { it.trim().removeSurrounding("\"") }
                urls.filter { it.isNotBlank() }.forEach { inspectUrl(it) }
            }
        }
    }

    private fun inspectUrl(url: String) {
        val lowerUrl = url.lowercase(Locale.ROOT)

        val stream = when {
            lowerUrl.contains(".m3u8") -> MediaStream(url, MediaType.STREAM_PLAYLIST, "application/x-mpegURL", "m3u8")
            lowerUrl.contains(".mpd") -> MediaStream(url, MediaType.STREAM_PLAYLIST, "application/dash+xml", "mpd")
            lowerUrl.contains(".mp4") -> MediaStream(url, MediaType.VIDEO, "video/mp4", "mp4")
            lowerUrl.contains(".webm") -> MediaStream(url, MediaType.VIDEO, "video/webm", "webm")
            lowerUrl.contains(".mp3") -> MediaStream(url, MediaType.AUDIO, "audio/mpeg", "mp3")
            lowerUrl.contains(".m4a") -> MediaStream(url, MediaType.AUDIO, "audio/mp4", "m4a")
            lowerUrl.contains(".aac") -> MediaStream(url, MediaType.AUDIO, "audio/aac", "aac")
            lowerUrl.contains(".ogg") -> MediaStream(url, MediaType.AUDIO, "audio/ogg", "ogg")
            else -> null
        }

        if (stream != null) {
            val currentList = _streams.value
            if (currentList.none { it.url == stream.url }) {
                _streams.value = currentList + stream
            }
        }
    }

    fun cleanUp() {
        mainHandler.post {
            webView?.stopLoading()
            webView?.destroy()
            webView = null
        }
    }
}
