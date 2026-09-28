package com.example.mediasniffer

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.widget.Toast

class MediaDownloader(private val context: Context) {

    fun download(stream: MediaStream) {
        try {
            val uri = Uri.parse(stream.url)
            val fileName = "media_${System.currentTimeMillis()}.${stream.extension}"

            val request = DownloadManager.Request(uri).apply {
                setTitle(fileName)
                setDescription("Downloading ${stream.type.name.lowercase()} file...")
                setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName)
                setMimeType(stream.mimeType)
                setAllowedOverMetered(true)
                setAllowedOverRoaming(true)
            }

            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            manager.enqueue(request)

            Toast.makeText(context, "Download started: $fileName", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(context, "Download failed: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }
}
